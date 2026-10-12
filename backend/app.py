from flask import Flask, jsonify
from flask_cors import CORS

from config import Config
from database.db import init_pool, run_query
from routes.auth import auth_bp
from routes.family import family_bp
from routes.transactions import transactions_bp

app = Flask(__name__)
CORS(app)


@app.route("/ping", methods=["GET"])
def ping():
    """Sanity check: confirms Flask is up and can talk to MySQL."""
    try:
        result = run_query("SELECT 1 AS ok", fetch_one=True)
        return jsonify({"status": "ok", "flask": "running", "mysql": result}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


# Register blueprints
app.register_blueprint(auth_bp)
# Next blueprints to add as you build them:
app.register_blueprint(family_bp)
app.register_blueprint(transactions_bp)
# app.register_blueprint(budget_bp)
# app.register_blueprint(dashboard_bp)


if __name__ == "__main__":
    init_pool()
    app.run(host="0.0.0.0", port=5000, debug=Config.DEBUG)