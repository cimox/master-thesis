import logging.config

from flask import Flask


app = Flask(__name__)
FORMAT = '%(asctime)-15s %(message)s'
logging.basicConfig(format=FORMAT, level=logging.INFO)
logger = logging.getLogger('API')
logger.info("API server has started")