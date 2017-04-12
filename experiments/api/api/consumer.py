import threading
import ujson

from api import logger
from kafka import KafkaConsumer


class Consumer(threading.Thread):
    daemon = True

    def __init__(self, topic, bootstrap_servers='localhost:9092'):
        super(Consumer, self).__init__()
        self.consumer = KafkaConsumer(bootstrap_servers=bootstrap_servers, auto_offset_reset='earliest')
        self.consumer.subscribe(topic)

    @staticmethod
    def deserialize_msg(msg):
        try:
            return ujson.loads(msg.value)
        except ValueError as e:
            logger.exception('While deserialization msg {}'.format(msg.value))

    def process_messages(self):
        for msg in self.consumer:
            logger.debug('Emitting message {}'.format(msg.value))
            yield 'data: {0}\n\n'.format(self.deserialize_msg(msg))
