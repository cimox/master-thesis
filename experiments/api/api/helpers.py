from api import logger


def deserialize_redis_msg(redis_msg):
    if not redis_msg:
        return 'none'

    if type(redis_msg) == list:
        try:
            return [float(e.decode('utf-8')) for e in redis_msg]
        except ValueError as e:
            logger.exception('Deserializing redis msg: {}'.format(redis_msg))
    return redis_msg.decode('utf-8')
