import logging.config

from flask import Flask
from flask_cors import CORS, cross_origin


app = Flask(__name__)
CORS(app)
FORMAT = '%(asctime)-15s %(message)s'
logging.basicConfig(format=FORMAT, level=logging.INFO)
logger = logging.getLogger('API')
logger.info("API server has started")