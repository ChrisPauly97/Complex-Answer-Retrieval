import os

# OR, the same with increased verbosity:

DEBUG = True
if os.getenv('DEBUG', '').lower() in ['0', 'no', 'false']:
    DEBUG = False

API_BIND_HOST = os.getenv('SERVICE_API_HOST', '0.0.0.0')
API_BIND_PORT = int(os.getenv('SERVICE_API_PORT', 5001))
