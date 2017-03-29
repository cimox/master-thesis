import argparse
import redis

from api import app, logger
from api.consumer import Consumer
from api.helpers import deserialize_redis_msg
from flask import jsonify

kafka_previous_msg = None


@app.route('/status/')
def status():
    return jsonify({
        'status': 'alive',
        'success': True,
    })


@app.route('/tree/')
def get_tree_increment():
    kafka_msg = kafka_messages.__next__()

    return jsonify({
        'data': {
            'tree': kafka_msg,
        },
        'success': True,
    })


@app.route('/node/<node_id>/stats/', methods=['GET'])
def node_stats(node_id):
    hoeffding_bound = redis.lrange('{}_hoeffdingBound'.format(node_id), 0, -1)
    alt_error_rate = redis.lrange('{}_altErrorRate'.format(node_id), 0, -1)
    old_error_rate = redis.lrange('{}_oldErrorRate'.format(node_id), 0, -1)
    tree_status = redis.get('{}_status'.format(node_id))

    return jsonify({
        'data': {
            'hoeffdingBound': deserialize_redis_msg(hoeffding_bound),
            'altErrorRate': deserialize_redis_msg(alt_error_rate),
            'oldErrorRate': deserialize_redis_msg(old_error_rate),
            'tree_status': deserialize_redis_msg(tree_status),
        },
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

    # Initialize Kafka consumer.
    kafka_consumer = Consumer(topic=args.topic)
    kafka_messages = kafka_consumer.process_messages()

    # Initialize Redis.
    redis = redis.StrictRedis(host=args.redis_host, port=args.redis_port, db=args.redis_db)

    # Start rest api.
    app.run(host=args.host, port=args.port)
