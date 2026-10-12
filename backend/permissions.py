from database.db import run_query

ADMIN_ONLY_IMPLICIT = True  # admins bypass the table entirely


def has_permission(user_id, family_id, permission_key):
    membership = run_query(
        "SELECT role FROM family_members WHERE family_id = %s AND user_id = %s",
        (family_id, user_id), fetch_one=True
    )
    if not membership:
        return False
    if membership["role"] == "ADMIN":
        return True

    granted = run_query(
        "SELECT id FROM member_permissions WHERE family_id = %s AND user_id = %s AND permission_key = %s",
        (family_id, user_id, permission_key), fetch_one=True
    )
    return granted is not None