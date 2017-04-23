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
        parentNode.isLeaf = false;
    }
}

function getTreeNode(tree, nodeID) {
    /*
     * BFT search tree for a given node ID
     */
    let queue = [];

    queue.push(tree);
    while (queue.length !== 0) {
        let element = queue.shift();
        if (_.isEqual(element.id, nodeID)) return element;

        if (element.children !== undefined) {
            for (let i = 0; i < element.children.length; i++) {
                queue.push(element.children[i]);
            }
        }
    }
    return undefined;
}

function updateNodeData(oldNode, newNode) {
    // TODO: this should be refactored
    // for (var key in newNode) {
    //     if (oldNode[key]) {
    //         oldNode[key] = newNode[key];
    //     }
    // }
    oldNode['weights'] = newNode['weights'];
    oldNode['hoeffdingBound'] = newNode['hoeffdingBound'];
    oldNode['nodeColor'] = newNode['nodeColor'];
}

function resolveCallback(callback, element) {
    return new Promise(resolve => {
        setTimeout(() => {
            return resolve(callback(element));
        }, 600);
    });
}

async function BFT(treeData, callback) {
    log.debug('Breadth-first search treeData');
    log.debug(treeData);

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
        resolve("success");
    });
}

function checkToRemoveChildrenOfNode(tree, actualNode) {
    /**
     * @param tree: current tree
     * @param actualNode: actual correct node
     */

    let treeNode = getTreeNode(tree, actualNode.id);
    if (treeNode !== undefined && actualNode.children.length < treeNode.children.length) {
        let actualNodeChildrenIds = new Set();

        // Get set of actual node children ids
        _.forEach(actualNode.children, function (child) {
            actualNodeChildrenIds.add(child.id)
        });

        // Remove old nodes from current tree
        for (let pos = 0; pos < treeNode.children.length; pos++) {
            let child = treeNode.children[pos];
            if (!actualNodeChildrenIds.has(child.id)) {
                // removeNodeLink(treeNode.children, child);
                // Remove old child once it's fade out
                log.debug('Removing ' + child.id);

                treeNode.children.splice(treeNode.children.indexOf(child), 1);
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

function getAllNodeIdsSet(tree) {
    let queue = [],
        result = new Set();

    queue.push(tree);
    while (queue.length !== 0) {
        let element = queue.shift();
        result.add(element.id);

        if (element.children !== undefined) {
            for (let i = 0; i < element.children.length; i++) {
                queue.push(element.children[i]);
            }
        }
    }
    return result;
}

function fadeOutAndRemove(nodeIdsToRemove, treeData, treeRoot) {
    if (nodeIdsToRemove.length > 1) {
        log.debug('List of nodes & links ids to remove: ' + nodeIdsToRemove);
        $('.node#' + nodeIdsToRemove.join(',.node#') + ',.link#' + nodeIdsToRemove.join(',.link#'))
            .css({"stroke": "red"})
            .fadeOut(fadeDuration, 'linear', function () {

                // Remove nodes from tree data model
                removeOldNodes(treeData, treeRoot);

                // Remove nodes and links from SVG DOM
                $(this).remove();

                updateTree();

                removalFinished = true;
            });
    }
    else if (nodeIdsToRemove.length == 1) {
        log.debug('Removing node and link: ' + nodeIdsToRemove);
        $('#' + nodeIdsToRemove[0] + '.node' + ',#' + nodeIdsToRemove[0] + '.link')
            .css({"stroke": "red"})
            .fadeOut(fadeDuration, 'linear', function () {

                // Remove nodes from tree data model
                removeOldNodes(treeData, treeRoot);

                // Remove nodes and links from SVG DOM
                $(this).remove();

                updateTree();

                removalFinished = true;
            });
    }
    else {
        log.debug('Nothing to remove!');
        removalFinished = true;
    }
}

function removeAndFadeOutOldNodes(treeData, treeRoot) {
    // Get correct node ids from tree data
    let correctIds = getAllNodeIdsSet(treeData);
    let stack = [], nodeIdsToRemove = [];

    // Get stack of visited nodes by DFS
    stack.push(treeRoot);
    while (stack.length !== 0) {
        let element = stack.pop();

        if (!correctIds.has(element.id)) {
            nodeIdsToRemove.push(element.id);
        }

        if (element.children !== undefined) {
            for (let i = 0; i < element.children.length; i++) {
                stack.push(element.children[i]);
            }
        }
    }

    fadeOutAndRemove(nodeIdsToRemove, treeData, treeRoot);
}

function endall(transition, callback) {
    if (typeof callback !== "function") throw new Error("Wrong callback in endall");
    if (transition.size() === 0) { callback() }
    var n = 0;
    transition
        .each(function() { ++n; })
        .each("end", function() { if (!--n) callback.apply(this, arguments); });
}