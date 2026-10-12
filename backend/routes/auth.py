import re
import bcrypt
from flask import Blueprint, request, jsonify
from database.db import run_query
from utils import generate_token, token_required

auth_bp = Blueprint("auth", __name__)

EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


@auth_bp.route("/register", methods=["POST"])
def register():
    data = request.get_json(silent=True) or {}
    name = (data.get("name") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""
    secret_code = (data.get("secret_code") or "").strip()

    if not name or not email or not password:
        return jsonify({"error": "name, email and password are required"}), 400
    if not EMAIL_REGEX.match(email):
        return jsonify({"error": "Invalid email format"}), 400
    if len(password) < 6:
        return jsonify({"error": "Password must be at least 6 characters"}), 400
    if secret_code and len(secret_code) < 4:
        return jsonify({"error": "Secret code must be at least 4 characters"}), 400

    existing = run_query("SELECT user_id FROM users WHERE email = %s", (email,), fetch_one=True)
    if existing:
        return jsonify({"error": "An account with this email already exists"}), 409

    password_hash = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
    secret_code_hash = None
    if secret_code:
        secret_code_hash = bcrypt.hashpw(secret_code.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

    user_id = run_query(
        "INSERT INTO users (name, email, password_hash, secret_code_hash) VALUES (%s, %s, %s, %s)",
        (name, email, password_hash, secret_code_hash),
        commit=True,
    )

    token = generate_token(user_id, email)
    return jsonify({
        "message": "Registered successfully",
        "token": token,
        "user": {"user_id": user_id, "name": name, "email": email}
    }), 201


@auth_bp.route("/verify-pin", methods=["POST"])
def verify_pin():
    data = request.get_json(silent=True) or {}
    email = (data.get("email") or "").strip().lower()
    secret_code = data.get("secret_code") or ""

    if not email or not secret_code:
        return jsonify({"error": "email and secret_code are required"}), 400

    user = run_query(
        "SELECT user_id, name, email, secret_code_hash FROM users WHERE email = %s",
        (email,), fetch_one=True
    )
    if not user or not user["secret_code_hash"]:
        return jsonify({"error": "No secret code set for this account"}), 401

    if not bcrypt.checkpw(secret_code.encode("utf-8"), user["secret_code_hash"].encode("utf-8")):
        return jsonify({"error": "Incorrect secret code"}), 401

    token = generate_token(user["user_id"], user["email"])
    return jsonify({
        "message": "PIN verified",
        "token": token,
        "user": {"user_id": user["user_id"], "name": user["name"], "email": user["email"]}
    }), 200


@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""

    if not email or not password:
        return jsonify({"error": "email and password are required"}), 400

    user = run_query(
        "SELECT user_id, name, email, password_hash FROM users WHERE email = %s",
        (email,), fetch_one=True
    )
    if not user:
        return jsonify({"error": "Invalid email or password"}), 401

    if not bcrypt.checkpw(password.encode("utf-8"), user["password_hash"].encode("utf-8")):
        return jsonify({"error": "Invalid email or password"}), 401

    token = generate_token(user["user_id"], user["email"])
    return jsonify({
        "message": "Login successful",
        "token": token,
        "user": {"user_id": user["user_id"], "name": user["name"], "email": user["email"]}
    }), 200


@auth_bp.route("/me", methods=["GET"])
@token_required
def me(current_user_id):
    user = run_query(
        "SELECT user_id, name, email, preferred_currency, created_at FROM users WHERE user_id = %s",
        (current_user_id,), fetch_one=True
    )
    if not user:
        return jsonify({"error": "User not found"}), 404
    return jsonify({"user": user}), 200
