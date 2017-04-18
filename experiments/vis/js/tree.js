function printTrainingTree(chart, timeout, currentTree, previousTree) {
    (function () {
        setTimeout(function () {
            root = currentTree;
            root.x0 = height / 2;
            root.y0 = 0;
            update(root);
        }, timeout);
    })();
}

function training(chart, timeout) {
    $.getJSON("data/tree-training_mini.json", function (trees) {
        console.log('Training...');
        var previousTree = undefined, currentTree = undefined;

        for (var i = 0; i < trees.length; i++) {
            currentTree = _.cloneDeep(trees[i]);
            printTrainingTree(chart, timeout * i, currentTree, previousTree);
            previousTree = _.cloneDeep(trees[i]);
        }

        console.log('Done! Read ' + trees.length + ' instances');
    });
};