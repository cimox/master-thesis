import ujson

from api import logger
from kafka import KafkaConsumer


class Consumer(object):

    def __init__(self, topic, bootstrap_servers='localhost:9092'):
        self.consumer = KafkaConsumer(bootstrap_servers=bootstrap_servers)
        self.consumer.subscribe(topic)

    @staticmethod
    def deserialize_msg(msg):
        try:
            return ujson.loads(msg.value)
        except ValueError as e:
            logger.exception('While deserialization msg {}'.format(msg.value))

    def process_messages(self):
        for msg in self.consumer:
            yield self.deserialize_msg(msg)
