import argparse
import signal
import redis
import ujson
import time

from api import logger
from api.consumer import Consumer
from api.helpers import deserialize_redis_msg, DataSource, BaseHandler, get_max_instances_seen
from tornado import gen
from tornado.httpserver import HTTPServer
from tornado.ioloop import IOLoop, PeriodicCallback
from tornado.iostream import StreamClosedError
from tornado.web import Application, url


class NodeStats(BaseHandler):
    def data_received(self, chunk):
        pass

    def get(self, node_id):
        node_id = node_id.replace('/', '')
        logger.info('Fetching node_id {} stats'.format(node_id))
        hoeffding_bound = redis.lrange('{}_hoeffdingBound'.format(node_id), 0, -1)
        # alt_error_rate = redis.lrange('{}_altErrorRate'.format(node_id), 0, -1)
        # old_error_rate = redis.lrange('{}_oldErrorRate'.format(node_id), 0, -1)
        tree_status = redis.get('{}_status'.format(node_id))

        self.write({
            'data': {
                'hoeffdingBound': deserialize_redis_msg(hoeffding_bound),
                'tree_status': deserialize_redis_msg(tree_status),
            },
            'success': True,
        })


class StreamProcessor(BaseHandler):
    """Tree increments server-sent events"""

    def data_received(self, chunk):
        pass

    def initialize(self, topic):
        """The ``source`` parameter is a string that is updated with
        new data. The :class:`EventSouce` instance will continuously
        check if it is updated and publish to clients when it is.
        """
        logger.info('Streaming tree increments from topic {}'.format(topic))
        self.kafka_consumer = Consumer(topic=topic)
        self.kafka_consumer.start()
        generator = self.kafka_consumer.process_messages()
        publisher = DataSource(next(generator))

        def get_next():
            publisher.data = next(generator)

        self.checker = PeriodicCallback(lambda: get_next(), 0.1)
        self.checker.start()
        self.source = publisher
        self._last = None
        self.set_header('content-type', 'text/event-stream')
        self.set_header('cache-control', 'no-cache')

    def on_finish(self):
        logger.info('Closing kafka consumer')
        self.kafka_consumer.close()
        self.checker.stop()

    def on_connection_close(self):
        logger.info('Closing kafka consumer')
        self.kafka_consumer.close()
        self.checker.stop()

    @gen.coroutine
    def publish(self, data):
        """Pushes data to a listener."""
        try:
            deserialized_data = ujson.loads(data)

            if deserialized_data.get('event') == 'notification':
                self.write('event: notification\ndata: {}\n\n'.format(data))
            elif deserialized_data.get('event') == 'tree':
                instances_seen_min_max = get_max_instances_seen(deserialized_data)

                deserialized_data['maxInstancesSeen'] = instances_seen_min_max.get('max')
                deserialized_data['minInstancesSeen'] = instances_seen_min_max.get('min')

                self.write('event: tree\ndata: {}\n\n'.format(ujson.dumps(deserialized_data)))
            else:
                pass

            yield self.flush()
        except StreamClosedError:
            pass

    @gen.coroutine
    def get(self):
        while True:
            if self.source.data != self._last:
                yield self.publish(self.source.data)
                self._last = self.source.data
            else:
                yield gen.sleep(0.005)


class MainHandler(BaseHandler):
    def data_received(self, chunk):
        pass

    def get(self):
        logger.info('Fetching API status')
        self.write({
            'status': 'alive',
            'success': True,
        })


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', help='web api host address', default='localhost', type=str)
    parser.add_argument('--port', help='web api port number', default=8080, type=int)
    parser.add_argument('--topic', help='kafka topic to subscribe', default='experiment', type=str)
    parser.add_argument('--redis_host', help='redis host', default='localhost', type=str)
    parser.add_argument('--redis_port', help='redis port', default=6379, type=int)
    parser.add_argument('--redis_db', help='redis database number', default=0, type=int)
    args = parser.parse_args()

    # Initialize Redis.
    # redis = redis.StrictRedis(host=args.redis_host, port=args.redis_port, db=args.redis_db)

    # Start tornado web app.
    app = Application([
        url(r'/', MainHandler),
        url(r'/status/', MainHandler),
        url(r'/stream/', StreamProcessor, dict(topic=args.topic)),
        url(r'/node/(.+)', NodeStats, name='node')
    ])
    server = HTTPServer(app)
    server.listen(8080)
    signal.signal(signal.SIGINT, lambda x, y: IOLoop.instance().stop())
    IOLoop.instance().start()
