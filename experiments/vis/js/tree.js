let circleRadius = 8;

let rectHeight = 23, rectHeightDivider = 1.5,
    rectWidthMultiplier = 5, rectWidthDivider = 2;

let linkMinWidth = 1, linkMinWidthLimit = 1,
    linkMaxWidth = 0, linkMaxWidthLimit = 12;

let alternatingTreeParents = [],
    notifiedAlternatingTreeParents = new Set();

function changesNotification() {
    if (alternatingTreeParents.length > 0) {
        let newChanges = 0;

        _.forEach(alternatingTreeParents, function(id) {
            if (!notifiedAlternatingTreeParents.has(id)) {
                notifiedAlternatingTreeParents.add(id);
                newChanges++;
            }
        });

        if (newChanges > 1) {
            $.notify("" + newChanges + " changes occurred.", "info");
        }
        else if (newChanges == 1) {
            $.notify("" + newChanges + " change occurred.", "info");
        }
    }
}

function addTreeIncrement(treeIncrement) {
    log.debug('Trying to add increment ' + treeIncrement.id + ' to tree');

    // Add a new node to its parent if exists.
    let treeNode = getTreeNode(root, treeIncrement.id);
    if (!root.children || root.children.length <= 0) {
        addChildren(root, treeIncrement);
    }
    else if (treeNode !== undefined && treeNode.id == treeIncrement.id && treeIncrement.id != "root") {
        log.debug('UPDATE node ' + treeIncrement.id);

        let nodeToUpdate = getTreeNode(root, treeIncrement.id);
        updateNodeData(nodeToUpdate, treeIncrement);

        updateTreeNodeLabels();
    }
    else if (treeIncrement.id != "root") {
        log.debug('ADD increment ' + treeIncrement.id + ' -> ' + treeIncrement.parentID);

        addChildren(getTreeNode(root, treeIncrement.parentID), treeIncrement);
    }

    // Notify if change occurred (alternating tree exists)
    if (treeIncrement.alternatingTree !== undefined) {
        alternatingTreeParents.push(treeIncrement.parentID);
        changesNotification();
    }

    // Get node and link values.
    let nodes = tree.nodes(root).reverse(),
        links = tree.links(nodes);

    let node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id;
        });
    let link = svg.selectAll(".link")
        .data(links, function (d) {
            return d.source.id + "-" + d.target.id;
        });

    // Render node labels and links between them.
    renderLinks(link, svg);
    renderNodes(node);
}

function updateTree() {
    // Get node and link values.
    let nodes = tree.nodes(root).reverse(),
        links = tree.links(nodes);

    let node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id;
        });
    let link = svg.selectAll("g.link")
        .data(links, function (d) {
            return d.source.id + "-" + d.target.id;
        });

    // Update nodes
    node.exit().remove()
        .transition()
        .duration(duration)
        .attr("transform", function (d) {
            d.px = d.x;
            d.py = d.y;
            return "translate(" + d.x + "," + d.y + ")";
        });

    node.transition()
        .duration(duration)
        .attr("transform", function (d) {
            d.px = d.x;
            d.py = d.y;
            return "translate(" + d.x + "," + d.y + ")";
        });

    node.select("circle")
        .style("fill", function (d) {
            return d.nodeColor ? d.nodeColor : "white";
        });

    node.select("text")
        .attr("dy", function (d) {
            if (d.isLeaf) return "1.5em";
            return "0.25em";
        })
        .attr("dx", function (d) {
            if (d.isLeaf) return "1.25em";
            return "-0.65em";
        })
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) {
                return "root";
            }
            else if (d.className) {
                return d.className;
            }
            return "";
        });

    // Update links
    link.exit().remove();

    svg.transition()
        .duration(duration)
        .selectAll(".link path")
        .attr("d", diagonal);
        // .attr("stroke-width", function (d) {
        //     let oldStrokeWidth = parseFloat(this.getAttribute("style").split(' ')[1].split(';')[0]);
        //     let newStrokeWidth = getLinkStrokeWidth(d);
        //
        //     if (newStrokeWidth > oldStrokeWidth) {
        //         return newStrokeWidth;
        //     }
        // });

    svg.transition()
        .duration(duration)
        .selectAll(".link rect")
        .attr("transform", function (d) {
            let rectXoffset = getRectWidth(d) / rectWidthDivider;
            let rectYoffset = rectHeight / rectHeightDivider;

            return "translate(" +
                ((d.source.x - rectXoffset + d.target.x - rectXoffset) / 2) + "," +
                ((d.source.y - rectYoffset + d.target.y - rectYoffset) / 2) + ")";
        });

    svg.transition()
        .duration(duration)
        .selectAll(".link text")
        .attr("transform", function (d) {
            return "translate(" +
                ((d.source.x + d.target.x) / 2) + "," +
                ((d.source.y + d.target.y) / 2) + ")";
        })
        .call(allTransitionFinished, function () {
            log.debug('Update animations finished.');
            animationsFinished = true;
        });
}

function updateTreeNodeLabels() {
    // Update node color and labels
    let nodes = tree.nodes(root).reverse();

    let node = svg.selectAll("g.node")
        .data(nodes, function (d) {
            return d.id;
        });

    node.select("circle")
        .style("fill", function (d) {
            return d.nodeColor ? d.nodeColor : "white";
        });

    node.select("text")
        .attr("dy", function (d) {
            if (d.isLeaf) return "1.5em";
            return "0.25em";
        })
        .attr("dx", function (d) {
            if (d.isLeaf) return "1.25em";
            return "-0.65em";
        })
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) {
                return "root";
            }
            else if (d.className) {
                return d.className;
            }
            return "";
        });

    // Keep root node in front
    node.select("#root").moveToFront();

    // Update link split stroke width
    let links = tree.links(nodes);
    let link = svg.selectAll("g.link")
        .data(links, function (d) {
            return d.source.id + "-" + d.target.id;
        });
    // link.selectAll("path")
    //     .attr("stroke-width", function (d) {
    //         let oldStrokeWidth = parseFloat(this.getAttribute("style").split(' ')[1].split(';')[0]);
    //         let newStrokeWidth = getLinkStrokeWidth(d);
    //
    //         if (newStrokeWidth > oldStrokeWidth) {
    //             return newStrokeWidth;
    //         }
    //     });
}

function renderNodes(node) {
    let nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.parent.px + "," + d.parent.py + ")";
        })
        .attr("id", function (d) {
            return d.id;
        });

    // Add entering nodes in the parent’s old position.
    nodeEnter.append("svg:circle")
        .attr("r", function (d) {
            return d.r | circleRadius;
        })
        .style("fill", function (d) {
            return d.nodeColor ? d.nodeColor : "white";
        });

    nodeEnter.append("svg:text")
        .attr("dy", function (d) {
            if (d.isLeaf) return "1.5em";
            return "0.25em";
        })
        .attr("dx", function (d) {
            if (d.isLeaf) return "1.25em";
            return "-0.65em";
        })
        .attr("text-anchor", function (d) {
            return d.children || d._children ? "end" : "start";
        })
        .style("font-family", "arial")
        .style("color", "black")
        .text(function (d) {
            if (_.isEqual(d.id, "root")) {
                return "root";
            }
            else if (d.className) {
                return d.className;
            }
            return "";
        });

    // Transition to the proper position for the node
    node.transition()
        .duration(duration)
        .attr("transform", function (d) {
            d.px = d.x;
            d.py = d.y;
            return "translate(" + d.x + "," + d.y + ")";
        });

    // Keep root node in front
    node.select("#root").moveToFront();
}

function renderLinks(link, svg) {
    // Add entering links in the parent’s old position.
    let linkEnter = link.enter().insert("g")
        .attr("class", "link")
        .attr("id", function (d) {
            return d.target.id;
        });

    linkEnter.append("path")
        .attr("id", function (d) {
            return d.target.id;
        })
        .attr("d", function (d) {
            let o = {
                x: d.source.px,
                y: d.source.py
            };
            return diagonal({source: o, target: o});
        });

    // Link path transition
    svg.transition()
        .duration(duration)
        .selectAll("path")
        .attr("id", function (d) {
            return d.target.id;
        })
        .attr("d", diagonal)
        .style("stroke-width", function (d) {
            return getLinkStrokeWidth(d);
        })
        .call(allTransitionFinished, function () {
            renderSplitRules(link);
        });

    // Link rectangle transition
    svg.transition()
        .duration(duration)
        .selectAll(".link rect")
        .attr("transform", function (d) {
            let rectXoffset = getRectWidth(d) / rectWidthDivider;
            let rectYoffset = rectHeight / rectHeightDivider;

            return "translate(" +
                ((d.source.x - rectXoffset + d.target.x - rectXoffset) / 2) + "," +
                ((d.source.y - rectYoffset + d.target.y - rectYoffset) / 2) + ")";
        });

    // Link text transition
    svg.transition()
        .duration(duration)
        .selectAll(".link text")
        .attr("transform", function (d) {
            return "translate(" +
                ((d.source.x + d.target.x) / 2) + "," +
                ((d.source.y + d.target.y) / 2) + ")";
        })
        .call(allTransitionFinished, function () {
            log.debug('Enter animations finished.');
            animationsFinished = true;
        });
}

d3.selection.prototype.moveToFront = function () {
    return this.each(function () {
        this.parentNode.appendChild(this);
    });
};

function renderSplitRules(link) {
    link.selectAll("rect").remove();
    let linkRect = link.append("svg:rect")
        .attr("id", function (d) {
            return d.target.id;
        })
        .attr("transform", function (d) {
            let rectXoffset = getRectWidth(d) / rectWidthDivider;
            let rectYoffset = rectHeight / rectHeightDivider;

            return "translate(" +
                ((d.source.x - rectXoffset + d.target.x - rectXoffset) / 2) + "," +
                ((d.source.y - rectYoffset + d.target.y - rectYoffset) / 2) + ")";
        })
        .attr("width", function (d) {
            return getRectWidth(d);
        })
        .attr("height", rectHeight * 2)
        .attr("rx", 2)
        .attr("ry", 2);

    link.selectAll("text").remove();
    let linkText = link.append("text")
        .attr("id", function (d) {
            return d.target.id;
        })
        .attr("transform", function (d) {
            return "translate(" +
                ((d.source.x + d.target.x) / 2) + "," +
                ((d.source.y + d.target.y) / 2) + ")";
        })
        .attr("dx", function (d) {
            return 0;
        })
        .attr("dy", function (d) {
            return 0;
        })
        .attr("text-anchor", "middle")
        .text(function (d) {
            return getSplitRuleText(d);
        })
        .call(wrap, 100)
        .on("mouseover", function(){
            // move to front
            console.log('moving on');
            this.parentNode.parentNode.appendChild(this.parentNode);
        });
}