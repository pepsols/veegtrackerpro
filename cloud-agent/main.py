import os
from flask import Flask, request, jsonify
import firebase_admin
from firebase_admin import credentials, firestore

app = Flask(__name__)

# Initialize Firebase Admin SDK
# On Cloud Run, it uses the default service account automatically
if not firebase_admin._apps:
    firebase_admin.initialize_app()

db = firestore.client()

@app.route('/')
def health():
    return "Veegtracker AI Agent is Online", 200

@app.route('/create_task', methods=['POST'])
def create_task():
    """AI or Admin can call this to inject a task into the cloud"""
    try:
        data = request.json
        task_name = data.get('name', 'Nieuwe Taak')

        task_ref = db.collection('tasks').document()
        task_ref.set({
            'name': task_name,
            'status': 'open',
            'createdAt': firestore.SERVER_TIMESTAMP,
            'source': 'ai_agent'
        })

        return jsonify({"status": "success", "id": task_ref.id}), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/status', methods=['GET'])
def get_status():
    """Get a summary of active operations for the AI"""
    try:
        routes = db.collection('routes').limit(10).get()
        pois = db.collection('pois').limit(10).get()

        summary = {
            "active_routes": len(routes),
            "recent_pois": len(pois),
            "system": "operational"
        }
        return jsonify(summary), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=int(os.environ.get("PORT", 8080)))
