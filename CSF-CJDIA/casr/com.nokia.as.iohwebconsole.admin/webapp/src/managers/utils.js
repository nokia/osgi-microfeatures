/*eslint no-extend-native: ["error", { "exceptions": ["String"] }]*/
if (!String.prototype.hashCode)
{
	String.prototype.hashCode = function() {
	  var hash = 0, i, chr, len;
	  if (this.length === 0) return hash;
	  len = this.length;
	  for (i = 0; i < len; i++) {
	    chr   = this.charCodeAt(i);
	    hash  = ((hash << 5) - hash) + chr;
	    hash |= 0; // Convert to 32bit integer
	  }
	  return hash;
	};
}

const Utils = 
{
		getUrlVars: function(){
		    var vars = [], hash;
		    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
		    for(var i = 0; i < hashes.length; i++)
		    {
		      hash = hashes[i].split('=');
		      vars.push(hash[0]);
		      vars[hash[0]] = hash[1];
		    }
		    return vars;
		},
		getUrlVar: function(name){
		    return Utils.getUrlVars()[name];
		}

};
export default Utils;
