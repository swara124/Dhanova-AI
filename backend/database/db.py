import mysql.connector
from mysql.connector import pooling
from config import Config

_pool = None


def init_pool():
    """Call this once when the app starts."""
    global _pool
    _pool = pooling.MySQLConnectionPool(
        pool_name="dhanova_pool",
        pool_size=5,
        host=Config.MYSQL_HOST,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DB,
        port=Config.MYSQL_PORT,
    )


def get_connection():
    if _pool is None:
        init_pool()
    return _pool.get_connection()


def run_query(query, params=None, fetch=False, fetch_one=False, commit=False):
    """
    Small helper so routes don't repeat boilerplate.
    - fetch=True        -> returns list of dict rows
    - fetch_one=True     -> returns single dict row (or None)
    - commit=True        -> commits and returns lastrowid
    """
    conn = get_connection()
    cursor = conn.cursor(dictionary=True)
    try:
        cursor.execute(query, params or ())
        result = None
        if fetch_one:
            result = cursor.fetchone()
        elif fetch:
            result = cursor.fetchall()
        if commit:
            conn.commit()
            result = cursor.lastrowid
        return result
    finally:
        cursor.close()
        conn.close()
