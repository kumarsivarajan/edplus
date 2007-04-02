About ExpertDesk Plus
---------------------
ExpertDesk Plus (edplus) is a Java library that simplifies AR System Java API development, licenced under the Apache License.
It acts as an add-on to the existing AR System API, not as a replacement. The features of the library are:

* Uses Java 5 generics and the collection framework, eg Iterators 
* A single entry point, the Server class for common tasks
* Can be "plugged in" to existing code without major refactoring

Installation & use
------------------
ExpertDesk Plus requires Java 5 or newer, because it uses Java 5 specific features, such as generics.

1. Set up your AR System API project as usual, using arapi63.jar or arapi70.jar.
2. Add the edplus-x.x.x.jar to the classpath of your project.
3. Use the library.

Compiling the library
---------------------
A build.xml ANT file is provided for compiling the library and generating the javadoc. To compile the library you'll need
a copy of the AR System Java API (only arapi63.jar or arapi70.jar) that is provided with the AR System.
Copy the arapixx.jar to the lib-rt directory and execute the build.xml file using ANT.

Websites
--------
ExpertDesk Plus is hosted on google code: http://code.google.com/p/edplus/
More information about ExpertDesk: http://www.expertdesk.com
More information about Mansystems: http://www.mansystems.nl
