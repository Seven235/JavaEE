import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ParseWebXml {
	private Map<String,String> servletMap= new HashMap<String,String>();
	
	@SuppressWarnings("unchecked")
	public void toParseXml(){
		SAXReader reader=new SAXReader();
		File file=new File("src/web.xml");
		try {
			
			Document document=reader.read(file);
			Element root = document.getRootElement();
			List<Element> childElements = root.elements();
			
			Map<String,String> servlet = new HashMap<String,String>();
			Map<String,String> servletMapping= new HashMap<String,String>();
			for (Element child : childElements) {
				if(child.getName()=="servlet"){
					Element servletName=child.element("servlet-name");
					Element servletClass=child.element("servlet-class");
					servlet.put(servletName.getText(),servletClass.getText());
				}
				if(child.getName()=="servlet-mapping"){
					Element servletName=child.element("servlet-name");
					Element urlPattern=child.element("url-pattern");
					servletMapping.put(servletName.getText(),urlPattern.getText());
				}
			}
			
			Iterator iter = servlet.entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
			    String value = (String) entry.getValue();
			    String otherKey=servletMapping.get(key);
			    if(!(otherKey==null)){
			    	servletMap.put(otherKey, value);
			    }
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String,String> getServletMap(){
		return servletMap;
	}
}
