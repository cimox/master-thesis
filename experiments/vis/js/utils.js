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

function resolveCallback(callback, element) {
    return new Promise(resolve => {
        setTimeout(() => {
            return resolve(callback(element));
        }, 750);
    });
}

async function BFT(treeData, callback) {
    let queue = [];

    queue.push(treeData);
    while (queue.length !== 0) {
        let element = queue.shift();
        log.debug('BFT processing ' + element.id);

        await resolveCallback(callback, element);

        if (element.children !== undefined) {
            for (let i = 0; i < element.children.length; i++) {
                queue.push(element.children[i]);
            }
        }
    }

    return new Promise(resolve => {
        setTimeout(() => {
            resolve("success");
        }, 50);
    });
}

function checkToRemoveChildrenOfNode(tree, actualNode) {
    /**
     * @param tree: current tree
     * @param actualNode: actual correct node
     */

    let treeNode = getTreeNode(tree, actualNode.id);
    if (actualNode.children.length < treeNode.children.length) {
        let actualNodeChildrenIds = new Set();

        // Get set of actual node children ids
        _.forEach(actualNode.children, function (child) {actualNodeChildrenIds.add(child.id)});

        // Remove old nodes from current tree
        for (let i = 0; i < treeNode.children.length; i++) {
            let child = treeNode.children[i];
            if (!actualNodeChildrenIds.has(child.id)) {
                log.debug('Removing child ' + child.id);

                treeNode.children.splice(i, 1); // Remove old child

                updateTree();
            }
        }
    }
}

function removeOldNodes(treeData, tree) {
    log.debug('Trying to remove old nodes');
    let queue = [];

    queue.push(treeData);
    while (queue.length !== 0) {
        let element = queue.shift();
        if (element.children !== undefined) checkToRemoveChildrenOfNode(tree, element);
        if (element.children !== undefined) {
            for (let i = 0; i < element.children.length; i++) {
                queue.push(element.children[i]);
            }
        }
    }
}