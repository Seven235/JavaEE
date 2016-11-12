package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class ClassPathXmlApplicationContext implements ApplicationContext{
	private String[] locations;
	private Map<String,Class<?>> beanMap;
	private Map<String,Map<String,String>> refMap;
	private Map<String,Map<String,String>> valueMap;
	private Map<String,Object> obMap;
	private Map<String,String> autowiredMap;
	
	public ClassPathXmlApplicationContext(String[] locations) {
		this.locations=locations;
		beanMap=new HashMap<String,Class<?>>();
		obMap=new HashMap<String,Object>();
		refMap=new HashMap<String,Map<String,String>>();
		valueMap=new HashMap<String,Map<String,String>>();
		autowiredMap=new HashMap<String,String>();
		getTotalComponent();
	}
	
	public Object getBean(String beanName) {
		Object beanOb=obMap.get(beanName);
		if(beanOb==null){
			Class<?> clazz=beanMap.get(beanName);
			try {
				String autowireField=autowiredMap.get(beanName);
				if(autowireField==null){
					beanOb = (Object)clazz.newInstance();
					if(valueMap.get(beanName)!=null){
						Iterator<?> iter = valueMap.get(beanName).entrySet().iterator();
						while(iter.hasNext()){
							Map.Entry<?,?> entry=(Map.Entry<?,?>)iter.next();
							String name=(String)entry.getKey();
							String value=(String)entry.getValue();
							Field field=clazz.getDeclaredField(name);
							Method method=clazz.getDeclaredMethod("set"+name.substring(0,1).toUpperCase()+name.substring(1),field.getType());
							method.invoke(beanOb,value);
						}
					}
					if(refMap.get(beanName)!=null){
						Iterator<?> iter=refMap.get(beanName).entrySet().iterator();
						while(iter.hasNext()){
							Map.Entry<?,?> entry=(Map.Entry<?,?>)iter.next();
							String name=(String)entry.getKey();
							String ref=(String)entry.getValue();
							Field field=clazz.getDeclaredField(name);
							Method method = clazz.getDeclaredMethod("set"+name.substring(0,1).toUpperCase()+name.substring(1),field.getType());
							Object obRef=obMap.get(ref);
							if(obRef==null){
								obRef=getBean(ref);
							}
							method.invoke(beanOb,obRef);
							obMap.put(ref,obRef);
						}
					}
				}else{
					List<String> fieldList=new ArrayList<String>();
					String[] fieldNameValue=autowireField.split(",");
					for(String fieldName:fieldNameValue){
						fieldList.add(fieldName.split(" ")[0]);
					}
					Class<?>[] paramTypes=new Class[fieldList.size()];
					Object[] params=new Object[fieldList.size()];
					for(int i=0;i<fieldList.size();i++){
						Field field=clazz.getDeclaredField(fieldList.get(i));
						paramTypes[i]=field.getType();
						String fieldName=field.getName();
						if((valueMap.get(beanName)!=null)&&(valueMap.get(beanName).get(fieldName)!=null)){
							params[i]=valueMap.get(beanName).get(fieldName);
						}else if((refMap.get(beanName)!=null)&&(refMap.get(beanName).get(fieldName)!=null)){
							String ref=refMap.get(beanName).get(fieldName);
							Object obRef=obMap.get(ref);
							if(obRef==null){
								obRef=getBean(ref);
							}
							params[i]=obRef;
						}
					}
					
					Constructor<?> con = clazz.getConstructor(paramTypes);
					beanOb=(Object)con.newInstance(params);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			obMap.put(beanName, beanOb);
		}
		return beanOb;
	}
	
	private void getTotalComponent(){
		File dir=new File(this.getClass().getResource("").getPath());
		while(true){
			File file=dir.getParentFile();
			if(file.getName().equals("bin")){
				File[] files=file.getParentFile().listFiles();
				for(File fl:files){
					if(fl.getName().equals("src")){
						dir=fl;
						break;
					}
				}
				break;
			}
		}
		File[] src=dir.listFiles();
		for(File fl:src){
			if(fl.getName().equals(locations[0]))
				toParseXML(fl);
			else if(fl.isDirectory())
				getFiles(fl,fl.getName());
		}
	}
	
	private void getFiles(File dir,String classPath){
		File[] files=dir.listFiles();
		for(File file:files){
			if(file.isDirectory()){
				classPath+=file.getName()+".";
				getFiles(file,classPath);
			}else{
				BufferedReader reader = null;
				try {
					reader=new BufferedReader(new FileReader(file));
					String tempString = null;
		            while ((tempString = reader.readLine()) != null) {
		                if(tempString.startsWith("@Component")){
		                	Class<?> clazz=Class.forName(classPath+"."+file.getName().split("[.]")[0]);
		                	Annotation[] annotations= clazz.getAnnotations();
		                	for(Annotation annotation:annotations){
		                		if(annotation.toString().startsWith("@test.Component")){
		                			Component c=(Component)annotation;
			                		beanMap.put(c.value(),clazz);
			                		break;
		                		}
		                	}
		                }
		                if(tempString.endsWith("@Autowired")){
		                	String fileName=file.getName().split("[.]")[0];
		                	tempString=reader.readLine();
		                	tempString=(tempString.substring(0,tempString.length()-2)).split("[(]")[1];
		                	autowiredMap.put(fileName.substring(0,1).toLowerCase()+fileName.substring(1),tempString);
		                }
		            }
		            reader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}finally {
		            if (reader != null) {
		                try {
		                    reader.close();
		                } catch (Exception e1) {
		                	e1.printStackTrace();
		                }
		            }
		        }
			}
		}
	}
	
	public void toParseXML(File file){
		SAXBuilder saxBuilder=new SAXBuilder();
		try {
			Document document=saxBuilder.build(file);
			Element beans=document.getRootElement();
			List<Element> bean=beans.getChildren();
			for(int i=0;i<bean.size();i++){
				Element theBean=bean.get(i);
				Class<?> clazz=Class.forName(theBean.getAttributeValue("class"));
				beanMap.put(theBean.getAttributeValue("id"),clazz);
				if(!theBean.getValue().equals("")){
					List<Element> property=theBean.getChildren();
					for(int j=0;j<property.size();j++){
						Element theProperty=property.get(j);
						if(theProperty.getAttributeValue("ref")!=null){
							Map<String,String> ref=refMap.get(theBean.getAttributeValue("id"));
							if(ref==null){
								ref=new HashMap<String,String>();
								ref.put(theProperty.getAttributeValue("name"),theProperty.getAttributeValue("ref"));
								refMap.put(theBean.getAttributeValue("id"),ref);
							}else{
								ref.put(theProperty.getAttributeValue("name"),theProperty.getAttributeValue("ref"));
							}
						}else if(theProperty.getAttributeValue("value")!=null){
							Map<String,String> value=valueMap.get(theBean.getAttributeValue("id"));
							if(value==null){
								value=new HashMap<String,String>();
								value.put(theProperty.getAttributeValue("name"),theProperty.getAttributeValue("value"));
								valueMap.put(theBean.getAttributeValue("id"),value);
							}else{
								value.put(theProperty.getAttributeValue("name"),theProperty.getAttributeValue("value"));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
