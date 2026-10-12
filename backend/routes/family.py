from flask import Blueprint, request, jsonify
from database.db import run_query
from utils import token_required
from permissions import has_permission
from mail_utils import send_family_invitation_email

family_bp = Blueprint("family", __name__)

VALID_PERMISSIONS = {
    "MANAGE_FAMILY", "MANAGE_MEMBERS", "ASSIGN_PERMISSIONS",
    "ADD_TRANSACTIONS", "EDIT_TRANSACTIONS", "DELETE_TRANSACTIONS",
    "MANAGE_BUDGETS", "MANAGE_GOALS", "CONTRIBUTE_GOALS", "VIEW_MEMBER_FINANCES"
}


@family_bp.route("/family/create", methods=["POST"])
@token_required
def create_family(current_user_id):
    data = request.get_json(silent=True) or {}
    family_name = (data.get("family_name") or "").strip()
    member_emails = data.get("member_emails") or []  # optional list of emails to invite right away

    if not family_name:
        return jsonify({"error": "family_name is required"}), 400

    existing_membership = run_query(
        "SELECT family_id FROM family_members WHERE user_id = %s",
        (current_user_id,), fetch_one=True
    )
    if existing_membership:
        return jsonify({"error": "You already belong to a family. Leave it before creating a new one."}), 409

    family_id = run_query(
        "INSERT INTO families (family_name, created_by) VALUES (%s, %s)",
        (family_name, current_user_id),
        commit=True,
    )

    # Creator automatically becomes ADMIN (admins implicitly have every permission)
    run_query(
        "INSERT INTO family_members (family_id, user_id, role) VALUES (%s, %s, 'ADMIN')",
        (family_id, current_user_id),
        commit=True,
    )

    invited = []
    skipped = []
    for email in member_emails:
        email = (email or "").strip().lower()
        if not email:
            continue
        user = run_query("SELECT user_id FROM users WHERE email = %s", (email,), fetch_one=True)
        if not user:
            skipped.append(email)  # not registered yet — can't invite someone with no account
            continue
        run_query(
            "INSERT INTO family_invitations (family_id, invited_email, invited_by) VALUES (%s, %s, %s)",
            (family_id, email, current_user_id),
            commit=True,
        )
        invited.append(email)

    return jsonify({
        "message": "Family created successfully",
        "family_id": family_id,
        "family_name": family_name,
        "invited": invited,
        "skipped_not_registered": skipped
    }), 201


@family_bp.route("/family/invite", methods=["POST"])
@token_required
def invite_member(current_user_id):
    data = request.get_json(silent=True) or {}
    family_id = data.get("family_id")
    email = (data.get("email") or "").strip().lower()

    if not family_id or not email:
        return jsonify({"error": "family_id and email are required"}), 400

    if not has_permission(current_user_id, family_id, "MANAGE_MEMBERS"):
        return jsonify({"error": "You don't have permission to invite members"}), 403

    existing = run_query(
        "SELECT invitation_id FROM family_invitations WHERE family_id = %s AND invited_email = %s AND status = 'PENDING'",
        (family_id, email), fetch_one=True
    )
    if existing:
        return jsonify({"error": "An invitation is already pending for this email"}), 409

    # No longer requires an existing account — anyone can be invited by email
    user = run_query("SELECT user_id FROM users WHERE email = %s", (email,), fetch_one=True)

    run_query(
        "INSERT INTO family_invitations (family_id, invited_email, invited_by) VALUES (%s, %s, %s)",
        (family_id, email, current_user_id),
        commit=True,
    )

    family = run_query("SELECT family_name FROM families WHERE family_id = %s", (family_id,), fetch_one=True)
    inviter = run_query("SELECT name FROM users WHERE user_id = %s", (current_user_id,), fetch_one=True)

    send_family_invitation_email(
        to_email=email,
        family_name=family["family_name"],
        inviter_name=inviter["name"],
        is_registered_user=(user is not None)
    )

    return jsonify({
        "message": "Invitation sent",
        "account_exists": user is not None
    }), 201


@family_bp.route("/family/invitations", methods=["GET"])
@token_required
def my_invitations(current_user_id):
    user = run_query("SELECT email FROM users WHERE user_id = %s", (current_user_id,), fetch_one=True)
    invitations = run_query(
        """SELECT fi.invitation_id, fi.family_id, f.family_name, u.name AS invited_by_name, fi.created_at
           FROM family_invitations fi
           JOIN families f ON fi.family_id = f.family_id
           JOIN users u ON fi.invited_by = u.user_id
           WHERE fi.invited_email = %s AND fi.status = 'PENDING'""",
        (user["email"],), fetch=True
    )
    return jsonify({"invitations": invitations}), 200


@family_bp.route("/family/invitations/<int:invitation_id>/respond", methods=["POST"])
@token_required
def respond_invitation(current_user_id, invitation_id):
    data = request.get_json(silent=True) or {}
    action = (data.get("action") or "").upper()  # "ACCEPT" or "REJECT"

    if action not in ("ACCEPT", "REJECT"):
        return jsonify({"error": "action must be ACCEPT or REJECT"}), 400

    invitation = run_query(
        "SELECT * FROM family_invitations WHERE invitation_id = %s AND status = 'PENDING'",
        (invitation_id,), fetch_one=True
    )
    if not invitation:
        return jsonify({"error": "Invitation not found or already handled"}), 404

    user = run_query("SELECT email FROM users WHERE user_id = %s", (current_user_id,), fetch_one=True)
    if user["email"] != invitation["invited_email"]:
        return jsonify({"error": "This invitation is not addressed to you"}), 403

    new_status = "ACCEPTED" if action == "ACCEPT" else "REJECTED"
    run_query(
        "UPDATE family_invitations SET status = %s WHERE invitation_id = %s",
        (new_status, invitation_id), commit=True
    )

    if action == "ACCEPT":
        existing_membership = run_query(
            "SELECT family_id FROM family_members WHERE user_id = %s",
            (current_user_id,), fetch_one=True
        )
        if existing_membership:
            return jsonify({"error": "You already belong to a family. Leave it before accepting a new invitation."}), 409

        run_query(
            "INSERT INTO family_members (family_id, user_id, role) VALUES (%s, %s, 'MEMBER')",
            (invitation["family_id"], current_user_id), commit=True
        )

    return jsonify({"message": f"Invitation {new_status.lower()}"}), 200


@family_bp.route("/family/<int:family_id>/members", methods=["GET"])
@token_required
def get_family_members(current_user_id, family_id):
    membership = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, current_user_id), fetch_one=True
    )
    if not membership:
        return jsonify({"error": "You are not a member of this family"}), 403

    # Any member can VIEW the member list — no permission gate needed for viewing
    members = run_query(
        """SELECT u.user_id, u.name, u.email, fm.role, fm.joined_at
           FROM family_members fm
           JOIN users u ON fm.user_id = u.user_id
           WHERE fm.family_id = %s""",
        (family_id,), fetch=True
    )
    return jsonify({"members": members}), 200


@family_bp.route("/family/<int:family_id>/members/<int:member_user_id>/permissions", methods=["GET"])
@token_required
def get_member_permissions(current_user_id, family_id, member_user_id):
    requester_membership = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, current_user_id), fetch_one=True
    )
    if not requester_membership:
        return jsonify({"error": "You are not a member of this family"}), 403

    target_membership = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, member_user_id), fetch_one=True
    )
    if not target_membership:
        return jsonify({"error": "That user is not a member of this family"}), 404

    if target_membership["role"] == "ADMIN":
        return jsonify({"role": "ADMIN", "permissions": list(VALID_PERMISSIONS)}), 200

    rows = run_query(
        "SELECT permission_key FROM member_permissions WHERE family_id = %s AND user_id = %s",
        (family_id, member_user_id), fetch=True
    )
    return jsonify({"role": "MEMBER", "permissions": [r["permission_key"] for r in rows]}), 200


@family_bp.route("/family/<int:family_id>/members/<int:member_user_id>/permissions", methods=["PUT"])
@token_required
def set_member_permissions(current_user_id, family_id, member_user_id):
    data = request.get_json(silent=True) or {}
    new_permissions = data.get("permissions")  # full list — replaces whatever existed

    if new_permissions is None or not isinstance(new_permissions, list):
        return jsonify({"error": "permissions must be a list"}), 400

    invalid = [p for p in new_permissions if p not in VALID_PERMISSIONS]
    if invalid:
        return jsonify({"error": f"Invalid permission keys: {invalid}"}), 400

    if current_user_id == member_user_id:
        return jsonify({"error": "You cannot modify your own permissions"}), 403

    if not has_permission(current_user_id, family_id, "ASSIGN_PERMISSIONS"):
        return jsonify({"error": "You don't have permission to assign permissions"}), 403

    target = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, member_user_id), fetch_one=True
    )
    if not target:
        return jsonify({"error": "That user is not a member of this family"}), 404
    if target["role"] == "ADMIN":
        return jsonify({"error": "Cannot modify an admin's permissions"}), 400

    run_query(
        "DELETE FROM member_permissions WHERE family_id = %s AND user_id = %s",
        (family_id, member_user_id), commit=True
    )
    for perm in new_permissions:
        run_query(
            "INSERT INTO member_permissions (family_id, user_id, permission_key, granted_by) VALUES (%s, %s, %s, %s)",
            (family_id, member_user_id, perm, current_user_id), commit=True
        )

    return jsonify({"message": "Permissions updated", "permissions": new_permissions}), 200


@family_bp.route("/family/my-families", methods=["GET"])
@token_required
def my_families(current_user_id):
    """Used by the app right after login to decide: Welcome screen or Dashboard."""
    families = run_query(
        """SELECT f.family_id, f.family_name, fm.role
           FROM family_members fm
           JOIN families f ON fm.family_id = f.family_id
           WHERE fm.user_id = %s""",
        (current_user_id,), fetch=True
    )
    return jsonify({"families": families}), 200

@family_bp.route("/family/<int:family_id>/members/<int:member_user_id>/role", methods=["PATCH"])
@token_required
def set_member_role(current_user_id, family_id, member_user_id):
    """
    A family has EXACTLY ONE admin. Making someone else admin is a TRANSFER:
    the target becomes ADMIN and the current admin becomes a regular MEMBER.
    """
    data = request.get_json(silent=True) or {}
    new_role = (data.get("role") or "").upper()

    if new_role != "ADMIN":
        return jsonify({"error": "Only role ADMIN is supported (it transfers the admin role). "
                                 "A family always has exactly one admin."}), 400

    # Caller must really BE the admin (not just hold a delegated permission)
    caller = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, current_user_id), fetch_one=True
    )
    if not caller or caller["role"] != "ADMIN":
        return jsonify({"error": "Only the family admin can transfer the admin role"}), 403

    if current_user_id == member_user_id:
        return jsonify({"error": "You are already the admin"}), 400

    target = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, member_user_id), fetch_one=True
    )
    if not target:
        return jsonify({"error": "That user is not a member of this family"}), 404

    # ONE atomic statement: target -> ADMIN, everyone else in the family -> MEMBER.
    # (Also self-heals any family that ended up with two admins earlier.)
    run_query(
        """UPDATE family_members
           SET role = CASE WHEN user_id = %s THEN 'ADMIN' ELSE 'MEMBER' END
           WHERE family_id = %s
           ORDER BY (role = 'ADMIN') DESC""",  # demote existing admin(s) first, then promote target
        (member_user_id, family_id), commit=True
    )

    # New admin bypasses permission checks, so explicit rows are meaningless for them.
    # The old admin starts as a plain member with a clean slate; the new admin decides
    # what (if anything) to grant them.
    run_query(
        "DELETE FROM member_permissions WHERE family_id = %s AND user_id IN (%s, %s)",
        (family_id, member_user_id, current_user_id), commit=True
    )

    return jsonify({"message": "Admin role transferred. You are now a regular member."}), 200