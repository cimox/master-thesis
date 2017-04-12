import logging.config

from flask import Flask
from flask_cors import CORS


app = Flask(__name__)
app.debug = False
CORS(app)
FORMAT = '%(asctime)-15s - %(name)s - %(levelname)s - %(message)s'
logging.basicConfig(format=FORMAT, level=logging.DEBUG)
logger = logging.getLogger('API')
logger.info("API server has started")