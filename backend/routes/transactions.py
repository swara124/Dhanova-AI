from flask import Blueprint, request, jsonify
from database.db import run_query
from utils import token_required
from permissions import has_permission
from currency_utils import convert_to_base

transactions_bp = Blueprint("transactions", __name__)


def get_user_family(user_id):
    return run_query(
        "SELECT family_id, role FROM family_members WHERE user_id = %s",
        (user_id,), fetch_one=True
    )


@transactions_bp.route("/transactions", methods=["POST"])
@token_required
def add_transaction(current_user_id):
    data = request.get_json(silent=True) or {}
    scope = (data.get("scope") or "").upper()  # "FAMILY" or "PERSONAL"
    txn_type = (data.get("type") or "").upper()  # "INCOME" or "EXPENSE"
    amount = data.get("amount")
    currency = (data.get("currency") or "INR").upper()
    category_id = data.get("category_id")
    description = data.get("description")
    payment_method = data.get("payment_method")
    transaction_date = data.get("transaction_date")

    if scope not in ("FAMILY", "PERSONAL") or txn_type not in ("INCOME", "EXPENSE"):
        return jsonify({"error": "scope must be FAMILY/PERSONAL, type must be INCOME/EXPENSE"}), 400
    if amount is None or not transaction_date:
        return jsonify({"error": "amount and transaction_date are required"}), 400

    membership = get_user_family(current_user_id)
    if not membership:
        return jsonify({"error": "You don't belong to a family"}), 403
    family_id = membership["family_id"]

    if scope == "FAMILY" and not has_permission(current_user_id, family_id, "ADD_TRANSACTIONS"):
        return jsonify({"error": "You don't have permission to add family transactions"}), 403
    # PERSONAL transactions: no permission needed, it's always your own data

    family = run_query("SELECT base_currency FROM families WHERE family_id = %s", (family_id,), fetch_one=True)
    base_currency = family["base_currency"] or "INR"
    base_amount, exchange_rate = convert_to_base(amount, currency, base_currency)

    transaction_id = run_query(
        """INSERT INTO transactions
           (family_id, user_id, scope, type, amount, currency, base_amount, base_currency,
            exchange_rate, category_id, description, payment_method, transaction_date)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (family_id, current_user_id, scope, txn_type, amount, currency, base_amount, base_currency,
         exchange_rate, category_id, description, payment_method, transaction_date),
        commit=True,
    )

    return jsonify({"message": "Transaction added", "transaction_id": transaction_id}), 201


@transactions_bp.route("/transactions", methods=["GET"])
@token_required
def get_transactions(current_user_id):
    scope = (request.args.get("scope") or "").upper()  # "FAMILY" or "PERSONAL"

    membership = get_user_family(current_user_id)
    if not membership:
        return jsonify({"error": "You don't belong to a family"}), 403
    family_id = membership["family_id"]

    if scope == "FAMILY":
        # Any member can VIEW family transactions — no gate on viewing
        rows = run_query(
            """SELECT t.*, u.name AS added_by_name FROM transactions t
               JOIN users u ON t.user_id = u.user_id
               WHERE t.family_id = %s AND t.scope = 'FAMILY'
               ORDER BY t.transaction_date DESC""",
            (family_id,), fetch=True
        )
    else:
        # PERSONAL: strictly your own rows only — enforced in the WHERE clause,
        # never filtered client-side (spec point 22)
        rows = run_query(
            """SELECT * FROM transactions
               WHERE user_id = %s AND scope = 'PERSONAL'
               ORDER BY transaction_date DESC""",
            (current_user_id,), fetch=True
        )

    return jsonify({"transactions": rows}), 200


@transactions_bp.route("/transactions/<int:transaction_id>", methods=["PUT"])
@token_required
def edit_transaction(current_user_id, transaction_id):
    data = request.get_json(silent=True) or {}

    txn = run_query("SELECT * FROM transactions WHERE transaction_id = %s", (transaction_id,), fetch_one=True)
    if not txn:
        return jsonify({"error": "Transaction not found"}), 404

    if txn["scope"] == "PERSONAL":
        if txn["user_id"] != current_user_id:
            return jsonify({"error": "You can only edit your own personal transactions"}), 403
    else:
        if not has_permission(current_user_id, txn["family_id"], "EDIT_TRANSACTIONS"):
            return jsonify({"error": "You don't have permission to edit family transactions"}), 403

    amount = data.get("amount", txn["amount"])
    currency = (data.get("currency") or txn["currency"]).upper()
    category_id = data.get("category_id", txn["category_id"])
    description = data.get("description", txn["description"])
    payment_method = data.get("payment_method", txn["payment_method"])
    transaction_date = data.get("transaction_date", txn["transaction_date"])

    family = run_query("SELECT base_currency FROM families WHERE family_id = %s", (txn["family_id"],), fetch_one=True)
    base_currency = family["base_currency"] or "INR"
    base_amount, exchange_rate = convert_to_base(amount, currency, base_currency)

    run_query(
        """UPDATE transactions SET amount=%s, currency=%s, base_amount=%s, base_currency=%s,
           exchange_rate=%s, category_id=%s, description=%s, payment_method=%s, transaction_date=%s
           WHERE transaction_id=%s""",
        (amount, currency, base_amount, base_currency, exchange_rate, category_id,
         description, payment_method, transaction_date, transaction_id),
        commit=True,
    )

    return jsonify({"message": "Transaction updated"}), 200


@transactions_bp.route("/transactions/<int:transaction_id>", methods=["DELETE"])
@token_required
def delete_transaction(current_user_id, transaction_id):
    txn = run_query("SELECT * FROM transactions WHERE transaction_id = %s", (transaction_id,), fetch_one=True)
    if not txn:
        return jsonify({"error": "Transaction not found"}), 404

    if txn["scope"] == "PERSONAL":
        if txn["user_id"] != current_user_id:
            return jsonify({"error": "You can only delete your own personal transactions"}), 403
    else:
        if not has_permission(current_user_id, txn["family_id"], "DELETE_TRANSACTIONS"):
            return jsonify({"error": "You don't have permission to delete family transactions"}), 403

    run_query("DELETE FROM transactions WHERE transaction_id = %s", (transaction_id,), commit=True)
    return jsonify({"message": "Transaction deleted"}), 200




def find_or_create_category(category_name, txn_type, family_id):
    if not category_name:
        return None
    existing = run_query(
        "SELECT category_id FROM categories WHERE name = %s AND type = %s AND (family_id = %s OR family_id IS NULL)",
        (category_name, txn_type, family_id), fetch_one=True
    )
    if existing:
        return existing["category_id"]
    return run_query(
        "INSERT INTO categories (name, type, is_default, family_id) VALUES (%s, %s, FALSE, %s)",
        (category_name, txn_type, family_id), commit=True
    )


@transactions_bp.route("/family/<int:family_id>/transactions", methods=["POST"])
@token_required
def add_family_transaction(current_user_id, family_id):
    data = request.get_json(silent=True) or {}
    txn_type = (data.get("type") or "").upper()
    amount = data.get("amount")
    category = data.get("category")
    title = (data.get("title") or "").strip()
    date = data.get("date")
    payment_method = data.get("payment_method")
    note = data.get("note")
    scope = (data.get("scope") or "FAMILY").upper()  # "FAMILY" (default) or "PERSONAL"

    if scope not in ("FAMILY", "PERSONAL"):
        return jsonify({"error": "scope must be FAMILY or PERSONAL"}), 400
    if txn_type not in ("INCOME", "EXPENSE"):
        return jsonify({"error": "type must be INCOME or EXPENSE"}), 400
    if amount is None or not title or not date:
        return jsonify({"error": "amount, title and date are required"}), 400

    membership = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, current_user_id), fetch_one=True
    )
    if not membership:
        return jsonify({"error": "You don't belong to this family"}), 403

    # Only FAMILY transactions need the permission. PERSONAL ones are always your own data.
    if scope == "FAMILY" and not has_permission(current_user_id, family_id, "ADD_TRANSACTIONS"):
        return jsonify({"error": "You don't have permission to add family transactions"}), 403

    family = run_query("SELECT base_currency FROM families WHERE family_id = %s", (family_id,), fetch_one=True)
    base_currency = family["base_currency"] or "INR"

    category_id = find_or_create_category(category, txn_type, family_id)

    transaction_id = run_query(
        """INSERT INTO transactions
           (family_id, user_id, scope, type, amount, currency, base_amount, base_currency,
            exchange_rate, category_id, description, payment_method, transaction_date, notes)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 1.0, %s, %s, %s, %s, %s)""",
        (family_id, current_user_id, scope, txn_type, amount, base_currency, amount, base_currency,
         category_id, title, payment_method, date, note),
        commit=True,
    )

    return jsonify({"message": "Transaction added", "transaction_id": transaction_id}), 201




@transactions_bp.route("/transactions/summary", methods=["GET"])
@token_required
def get_summary(current_user_id):
    """Powers the Dashboard: family totals (visible to all members) + your own personal totals."""
    membership = get_user_family(current_user_id)
    if not membership:
        return jsonify({"error": "You don't belong to a family"}), 403
    family_id = membership["family_id"]

    family_income = run_query(
        "SELECT COALESCE(SUM(base_amount),0) AS total FROM transactions WHERE family_id=%s AND scope='FAMILY' AND type='INCOME'",
        (family_id,), fetch_one=True
    )
    family_expense = run_query(
        "SELECT COALESCE(SUM(base_amount),0) AS total FROM transactions WHERE family_id=%s AND scope='FAMILY' AND type='EXPENSE'",
        (family_id,), fetch_one=True
    )
    personal_income = run_query(
        "SELECT COALESCE(SUM(base_amount),0) AS total FROM transactions WHERE user_id=%s AND scope='PERSONAL' AND type='INCOME'",
        (current_user_id,), fetch_one=True
    )
    personal_expense = run_query(
        "SELECT COALESCE(SUM(base_amount),0) AS total FROM transactions WHERE user_id=%s AND scope='PERSONAL' AND type='EXPENSE'",
        (current_user_id,), fetch_one=True
    )

    return jsonify({
        "family_income": float(family_income["total"]),
        "family_expense": float(family_expense["total"]),
        "family_balance": float(family_income["total"]) - float(family_expense["total"]),
        "personal_income": float(personal_income["total"]),
        "personal_expense": float(personal_expense["total"]),
        "personal_balance": float(personal_income["total"]) - float(personal_expense["total"]),
    }), 200