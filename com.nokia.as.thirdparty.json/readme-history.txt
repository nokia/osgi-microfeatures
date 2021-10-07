*****************************************************
JSONObject.java modified by apanday on July 13th 2011
*****************************************************

@@ -239,10 +239,7 @@
             Iterator i = map.entrySet().iterator();
             while (i.hasNext()) {
                 Map.Entry e = (Map.Entry)i.next();
-                Object value = e.getValue();
-                if (value != null) {
-                    this.map.put(e.getKey(), wrap(value));
-                }
+                this.map.put(e.getKey(), wrap(e.getValue()));
             }
         }
     }
@@ -1096,12 +1093,8 @@
         if (key == null) {
             throw new JSONException("Null key.");
         }
-        if (value != null) {
-            testValidity(value);
-            this.map.put(key, value);
-        } else {
-            remove(key);
-        }
+        testValidity(value);
+        this.map.put(key, wrap(value));
         return this;
     }
 
@@ -1629,4 +1622,4 @@
             throw new JSONException(exception);
         }
      }
-}
\ No newline at end of file
+}

*****************************************************
JSONObject.java modified by gtixier on July 21st 2011
*****************************************************

1546a1547,1553
+ 	 return wrap (object, true); // keep json.org def behavior
+      }
+ 
+     /**
+      * Added by ASR team to force object.toString without trying introspection
+      */
+     public static Object wrap(Object object, boolean introspect) {
1569a1577,1579
+ 	     if (introspect == false){
+ 		 return object.toString();
+ 	     }

*****************************************************
JSONObject.java modified by apanday on July 26th 2011
*****************************************************

635a636,638
>         else if (isNull(key)) {
>             return JSONObject.NULL.toString();
>         }

*******************************************************
JSONObject.java modified by apanday on August 29th 2011
*******************************************************

Index: Json/org/json/JSONObject.java
===================================================================
1550c1550
<        return wrap (object, true); // keep json.org def behavior
---
>        return wrap (object, Boolean.getBoolean("org.json.introspect")); // json.org def behavior was true

