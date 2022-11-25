package server;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {
	
	String function;
	HashMap<String, String> mapping = new HashMap<>();
	
	String get(String s) {
		return mapping.get(s);
	}
	String get(int i) {
		return mapping.get(i+"");
	}
	Message getMsg(String s) {
		return decode(mapping.get(s));
	}
	Message getMsg(int i) {
		return decode(mapping.get(i+""));
	}
	
	void put(String key, String val) {
		mapping.put(key, val);
	}
	void add(String ...strings) {
		int i = 0;
		while(mapping.containsKey(i+"")) i++;
		for(String s : strings) mapping.put(i+++"", s);
	}
	
	public Message() {
		
	}
	public Message(String fun, Map<String, String> mapping, String...strings) {
		this.function = fun;
		this.mapping.putAll(mapping);
		int i = 0;
		for(String s : strings) this.mapping.put(i+++"", s);
	}
	public Message(String fun, String...strings) {
		this.function = fun;
		int i = 0;
		for(String s : strings) this.mapping.put(i+++"", s);
	}
	public Message(String fun, Map<String, String> mapping) {
		this.function = fun;
		this.mapping.putAll(mapping);
	}
	
	static String revEscape(String s) {
		return s
				.replaceAll("\\\\,", ",")
				.replaceAll("\\\\=", "=")
				.replaceAll("\\\\\\\\", "\\\\")
				;
	}
	static String escape(String s) {
		return s
				.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll("=", "\\\\=")
				.replaceAll(",", "\\\\,")
		;
	}
	
	static Message decode(String msg) {
		Matcher functionMatcher = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_]*)\\((.*)\\)$").matcher(msg);
		if(functionMatcher.find()) {
			Message message = new Message(functionMatcher.group(1));
			String parse = functionMatcher.group(2);
			String[] strings = parse.split("(?<!([^\\\\]|^)\\\\(\\\\\\\\){0,99999999}),");
			int index = 0;
			for(String s : strings) {
				String[] s2 = s.split("(?<!([^\\\\]|^)\\\\(\\\\\\\\){0,99999999})=", 2);
				if(s2.length == 1) {
					message.mapping.put(index+"", revEscape(s2[0]));
					index++;
				} else if(s2.length == 2) {
					
					if(!s2[0].matches("[a-zA-Z][a-zA-Z0-9_]*"))
						throw new IllegalArgumentException(String.format("Invalid key: '%s'", s2[0]));
					message.mapping.put(s2[0], revEscape(s2[1]));
				}
			}
			
			return message;
		}
		throw new IllegalArgumentException(String.format("Invalid message format: '%s'", msg));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(function + "");
		sb.append('(');
		boolean sep = false;
		for(int i = 0; mapping.containsKey(i+""); i++) {
			String val = mapping.get(i+"");
			if(sep) sb.append(',');
			sb.append(escape(val));
			sep = true;
		}
		for(Entry<String, String> e : mapping.entrySet()) {
			if(e.getKey().matches("[0-9]+"))
				continue;
			if(sep) sb.append(',');
			sb.append(e.getKey());
			sb.append('=');
//			sb.append('\'');
			sb.append(escape(e.getValue()));
//			sb.append('\'');
			sep = true;
		}
		sb.append(')');
		return sb.toString();
	}
	
	public static void main(String[] args) {
//		Message m1 = decode("fun(\\,10,11,\\\\hello\\\\,world\\, just wanna say hi!,abc)");
//		System.out.println(m1);
//		System.out.println(m1.get(0));
//		System.out.println(m1.get(1));
//		System.out.println(m1.get(2));
//		System.out.println(m1.get(3));
//		Message m1 = decode("fun(player(name\\=anthony?!\\\\\\\\\\\\\\,][\\\\\\=\\,10),player(name\\=nicholas?!\\\\\\,][\\,10))");
//		System.out.println(m1);
//		System.out.println(m1.get("0"));
//		System.out.println(decode(m1.get("0")));
//		System.out.println(escape(decode(m1.get("0")).get(0)));
//		System.out.println(m1.getMsg(0));
//		
//		System.out.println(new Message("player", Map.of("name", "anthony?!\\,][="), "10"));
		Message msg = new Message("player");
		msg.add("kamron", "wachtwoord123", "10", "blue");
		msg.put("pass", "wachtwoord123");
		msg.put("name", "kamron");
		System.out.println(msg);
		
	}
}
