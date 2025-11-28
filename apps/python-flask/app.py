from flask import Flask, jsonify, request
from datetime import datetime

app = Flask(__name__)

# Sample data
users = [
    {"id": 1, "name": "John Doe", "email": "john@example.com"},
    {"id": 2, "name": "Jane Smith", "email": "jane@example.com"}
]

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "success": True,
        "message": "API is running",
        "timestamp": datetime.utcnow().isoformat() + "Z"
    })

@app.route('/api/health', methods=['GET'])
def api_health():
    return jsonify({
        "success": True,
        "message": "API is running",
        "timestamp": datetime.utcnow().isoformat() + "Z"
    })

@app.route('/api/users', methods=['GET'])
def get_users():
    return jsonify({
        "success": True,
        "data": users
    })

@app.route('/api/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    user = next((u for u in users if u["id"] == user_id), None)
    if user:
        return jsonify({"success": True, "data": user})
    return jsonify({"success": False, "message": "User not found"}), 404

@app.route('/api/users', methods=['POST'])
def create_user():
    data = request.get_json()
    new_user = {
        "id": len(users) + 1,
        "name": data.get("name"),
        "email": data.get("email")
    }
    users.append(new_user)
    return jsonify({"success": True, "data": new_user}), 201

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
