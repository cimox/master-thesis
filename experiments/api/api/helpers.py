from api import logger


def deserialize_redis_msg(msg):
    if type(msg) == list:
        try:
            return [float(e.decode('utf-8')) for e in msg]
        except ValueError as e:
            logger.exception('Deserializing redis msg: {}'.format(msg))
    return msg.decode('utf-8')
