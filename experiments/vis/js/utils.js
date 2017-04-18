function hasSameProperties(obj1, obj2 ) {
    if (obj1 === undefined || obj2 === undefined) return false;
    return Object.keys( obj1 ).every( function( property ) {
        if (typeof obj1[property] !== 'object') {
            return obj2.hasOwnProperty( property );
        } else {
            return hasSameProperties(obj1[property], obj2[property]);
        }
    });
}