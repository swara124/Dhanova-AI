import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # MySQL settings
    MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
    MYSQL_USER = os.getenv("MYSQL_USER", "root")
    MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "root")
    MYSQL_DB = os.getenv("MYSQL_DB", "dhanova_ai")
    MYSQL_PORT = int(os.getenv("MYSQL_PORT", 3306))

    # JWT settings
    JWT_SECRET = os.getenv("JWT_SECRET", "change-this-secret-key-in-production")
    JWT_EXPIRY_HOURS = int(os.getenv("JWT_EXPIRY_HOURS", 24 * 7))  # 7 days

    DEBUG = os.getenv("FLASK_DEBUG", "True") == "True"