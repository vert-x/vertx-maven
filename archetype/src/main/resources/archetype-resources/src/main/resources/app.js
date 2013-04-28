var container = require("container")
var console = require("console")

container.deployVerticle("${package}.MyVerticle")

console.log("Deploying myverticle")
