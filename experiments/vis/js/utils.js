function hasSameProperties(obj1, obj2) {
    if (obj1 === undefined || obj2 === undefined) return false;
    return Object.keys(obj1).every(function (property) {
        if (typeof obj1[property] !== 'object') {
            return obj2.hasOwnProperty(property);
        } else {
            return hasSameProperties(obj1[property], obj2[property]);
        }
    });
}

function addChildren(parentNode, element) {
    if (parentNode.children) {
        var newNode = _.cloneDeep(element);
        newNode['children'] = [];

        parentNode.children.push(newNode);
    }
    else {
        parentNode.id = "root";
        parentNode.children = [];
        parentNode.leaf = false;
    }
}

function getTreeNode(tree, nodeID) {
    /*
     * BFT search tree for a given node ID
     */
    var queue = [];

    queue.push(tree);
    while (queue.length !== 0) {
        var element = queue.shift();
        if (_.isEqual(element.id, nodeID)) return element;

        if (element.children !== undefined) {
            for (var i = 0; i < element.children.length; i++) {
                queue.push(element.children[i]);
            }
        }
    }
    return tree;
}

function updateNodeData(oldNode, newNode) {
    for (var key in newNode) {
        if (oldNode[key]) {
            oldNode[key] = newNode[key];
        }
    }
}

var timeout;
function bft(tree, callback, timeoutIncrement) {
    var queue = [];

    queue.push(tree);
    while (queue.length !== 0) {
        var element = queue.shift();
        function renderElement(element, timeout) {
            (function () {
                setTimeout(function () {
                    callback(element);
                }, timeout);
            })();
        }

        renderElement(element, timeout);
        timeout += timeoutIncrement;

        if (element.children !== undefined) {
            for (var i = 0; i < element.children.length; i++) {
                queue.push(element.children[i]);
            }
        }
    }
}