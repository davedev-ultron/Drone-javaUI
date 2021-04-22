const renderMapPointDataComponent = function (pointId, alt, speed) {
    // what will show up inside of the popup on the marker in google maps
    // in reality want to do this in react
    return '<form id="' + pointId + '">' +
        '<input type="text" name="height" value="' + alt + '"/> Height' + '<br />' +
        '<input type="text" name="speed" value="' + speed + '" /> Speed' + '<br />' +
        '<input type="hidden" name="key" value="' + pointId + '"/>' +
        '<select name="action"><option value="0">No Action</option><option value="1">Activate</option></select>' + '<br />' +
        '<input type="submit" value="Save" onClick="updatePointValue(this.form); return false;" />' +
        '<input type="button" value="Remove" onClick="removePoint(this.form); return false;" />' + '<br />' +
        '</form>'
}
