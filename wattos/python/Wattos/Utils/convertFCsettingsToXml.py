import xmlrpclib

d = dict(foo="Foo!", bar="Bar!")
#>>> import xml.etree.ElementTree as ET
# >>> e = ET.Element("dict")
# >>> for k in d:
#...     ET.SubElement(e, k).text = d[k]
#...
# >>> ET.tostring(e)
#'<dict><foo>Foo!</foo><bar>Bar!</bar></dict>'

print xmlrpclib.dumps((d,))