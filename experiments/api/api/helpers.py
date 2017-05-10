import sys

from api import logger
from tornado.web import RequestHandler


def deserialize_redis_msg(redis_msg):
    if not redis_msg:
        return 'none'

    if type(redis_msg) == list:
        try:
            return [float(e.decode('utf-8')) for e in redis_msg]
        except ValueError as e:
            logger.exception('Deserializing redis msg: {}'.format(redis_msg))
    return redis_msg.decode('utf-8')


class DataSource(object):
    """Source: https://gist.github.com/mivade/d474e0540036d873047f"""
    """Generic object for producing data to feed to clients."""

    def __init__(self, initial_data=None):
        self._data = initial_data

    @property
    def data(self):
        return self._data

    @data.setter
    def data(self, new_data):
        self._data = new_data


class BaseHandler(RequestHandler):
    def set_default_headers(self):
        self.set_header("Access-Control-Allow-Origin", "*")
        self.set_header("Access-Control-Allow-Headers", "x-requested-with")
        self.set_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')

    def post(self):
        pass

    def get(self):
        pass

    def options(self):
        self.set_status(204)
        self.finish()


def get_max_instances_seen(data):
    instances_seen_count = {'min': sys.maxsize, 'max': 0}

    def read_node_children(children):
        for child in children:
            if child.get('children'):
                read_node_children(child.get('children'))
            instances_seen = child.get('instancesSeen')
            if instances_seen:
                if instances_seen > instances_seen_count['max']:
                    instances_seen_count['max'] = instances_seen
                if instances_seen < instances_seen_count['min']:
                    instances_seen_count['min'] = instances_seen

    if data.get('children'):
        read_node_children(data.get('children'))
    return instances_seen_count
