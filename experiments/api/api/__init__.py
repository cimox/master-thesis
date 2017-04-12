import logging.config

FORMAT = '%(asctime)-15s - %(name)s - %(levelname)s - %(message)s'
logging.basicConfig(format=FORMAT, level=logging.INFO)
logger = logging.getLogger('API')
logger.info("API server has started")
