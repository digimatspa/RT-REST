/*
 * Copyright (C) 2012  Benjamin Boksa (http://www.boksa.de/)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package de.boksa.rt.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.boksa.rt.rest.response.parser.MultilineTicketSearchResponseParser;
import de.boksa.rt.rest.response.parser.RTParserModern;
import de.boksa.rt.rest.response.parser.processor.FieldProcessor;
import de.boksa.rt.rest.response.parser.processor.FieldProcessorRegistry;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.boksa.rt.model.RTTicket;
import de.boksa.rt.model.RTTicketAttachment;
import de.boksa.rt.model.RTTicketHistory;
import de.boksa.rt.model.RTTicketUser;
import de.boksa.rt.rest.RTRESTClient;
import de.boksa.rt.rest.RTRESTResponse;
import de.boksa.rt.rest.RTRESTClient.TicketSearchResponseFormat;
import de.boksa.rt.rest.response.parser.RTParser;
import de.boksa.rt.rest.response.parser.customconverters.StringToDateTimeConverter;

public class RESTRTTicketDAO implements RTTicketDAO {

	private RTRESTClient client;
	private StringToDateTimeConverter dateConverter;

	protected RESTRTTicketDAO() {
		ConvertUtils.deregister(DateTime.class);
		dateConverter = new StringToDateTimeConverter();
		ConvertUtils.register(dateConverter, DateTime.class);
	}

	@Override
	public boolean createNewTicket(RTTicket ticket, String text) throws Exception {
		Pattern PATTERN_TICKET_CREATED = Pattern.compile("^# Ticket (\\d+) created.$");
		client.login();
		RTRESTResponse response = client.newTicket(ticket, text);
		client.logout();

		Matcher m = PATTERN_TICKET_CREATED.matcher(response.getBody().trim());
		if (response.getStatusCode() == 200l && m.matches()) {
			long ticketId = Long.valueOf(m.group(1));
			ticket.setId(ticketId);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public RTTicket getTicket(long ticketId) throws Exception {
		client.login();
		RTRESTResponse response = client.getTicket(ticketId);
		client.logout();
		RTParser parser = RTParserModern.getInstance();

		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				List<Map<String, String>> attributes = parser.parseResponse(response);
				RTTicket ticket = new RTTicket();
				ticket.populate(attributes.get(0));
				//The parser doesn't parse out the ticket id properly bc RT is dumb and also I'm lazy
				ticket.setId(ticketId);
				return ticket;
			} else {
				// returning an empty ticket or null?
				return new RTTicket();
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}
	}

	@Override
	public RTTicketHistory getHistory(long history_id) throws IOException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public List<RTTicketHistory> findHistory(RTTicket ticket) throws Exception {
		client.login();
		RTRESTResponse response = client.getTicket(ticket.getId(), "history?format=l");
		client.logout();
		RTParser parser = RTParserModern.getInstance();

		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				List<Map<String, String>> attributes = parser.parseResponse(response);
				List<RTTicketHistory> historyList = new ArrayList<RTTicketHistory>();
				for (Map<String, String> historyAttribute : attributes) {
					RTTicketHistory history = new RTTicketHistory();
					history.populate(historyAttribute);
					historyList.add(history);
				}
				return historyList;
			} else {
				return new ArrayList<RTTicketHistory>();
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}
	}

	@Override
	public List<RTTicketHistory> findHistory(long ticketId) throws Exception {
		client.login();
		RTRESTResponse response = client.getTicket(ticketId, "history?format=l");
		client.logout();
		RTParser parser = RTParserModern.getInstance();

		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				List<Map<String, String>> attributes = parser.parseResponse(response);
				List<RTTicketHistory> historyList = new ArrayList<RTTicketHistory>();
				for (Map<String, String> historyAttribute : attributes) {
					RTTicketHistory history = new RTTicketHistory();
					history.populate(historyAttribute);
					historyList.add(history);
				}
				return historyList;
			} else {
				return new ArrayList<RTTicketHistory>();
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}
	}

	@Override
	public RTTicketAttachment findAttachment(long attachment_id) throws IOException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public List<RTTicketAttachment> findAttachment(RTTicket ticket) throws IOException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public RTTicketUser findUser(long user_id) throws IOException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public RTTicketUser findUser(String username) throws Exception {
		client.login();
		RTRESTResponse response = client.getUser(username);
		client.logout();
		RTParser parser = RTParserModern.getInstance();

		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				RTTicketUser user = new RTTicketUser();
				List<Map<String, String>> attributes = parser.parseResponse(response);
				for (Map<String, String> attribute : attributes) {
					BeanUtils.populate(user, attribute);
				}
				return user;
			} else {
				// No matches were found...just return an empty user instead of bombing out like a dumb
				return new RTTicketUser();
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}
	}

	@Override
	public List<RTTicket> findByQuery(String query) throws Exception {
		return this.findByQuery(query, null, RTRESTClient.TicketSearchResponseFormat.MULTILINE);
	}

	@Override
	public List<RTTicket> findByQuery(String query, RTRESTClient.TicketSearchResponseFormat formant) throws Exception {
		return this.findByQuery(query, null, RTRESTClient.TicketSearchResponseFormat.MULTILINE);
	}

	@Override
	//TODO refactor to return an actual ticket...have to do findById
	public List<RTTicket> findByQuery(String query, String orderby) throws Exception {
		client.login();
		RTRESTResponse response = client.searchTickets(query, orderby);
		client.logout();
		RTParser parser = RTParserModern.getInstance();

		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				List<Map<String, String>> parsedResponse = parser.parseResponse(response);
				List<RTTicket> tickets = new ArrayList<RTTicket>();

				for (Map<String, String> ticketData : parsedResponse) {
					for (String ticketId : ticketData.keySet()) {
						tickets.add(getTicket(Long.valueOf(ticketId)));
					}
				}

				return tickets;
			} else {
				// No matches were found...just return an empty list instead of bombing out like a dumb
				return new ArrayList<RTTicket>();
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}
	}

	@Override
	//TODO refactor to return an actual ticket...have to do findById
	public List<RTTicket> findByQuery(String query, String orderby, RTRESTClient.TicketSearchResponseFormat format) throws Exception {
		client.login();
		RTRESTResponse response = client.searchTickets(query, orderby, format);
		client.logout();
		RTParser parser= null;

		switch (format) {
			case IDONLY: parser = null; break;
			case IDANDSUBJECT: parser = null; break;
			case MULTILINE: parser = MultilineTicketSearchResponseParser.getInstance(); break;
		}


		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				List<Map<String, String>> parsedResponse = parser.parseResponse(response);

				return(processResultData(parsedResponse));

			} else {
				// No matches were found...just return an empty list instead of bombing out like a dumb
				return new ArrayList<RTTicket>();
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}
	}

	@Override
	public boolean resolveByTicketId(long ticketId) throws Exception {
		String content= "Status: resolved\n";
		return(this.updateByTicketId(ticketId, content));
	}

	@Override
	public boolean updateByTicketId(long ticketId, String content) throws Exception {
		client.login();
		RTRESTResponse response = client.updateTicket(Long.toString(ticketId), content);
		client.logout();
		RTParser parser = RTParserModern.getInstance();

		if (parser != null) {
			if (response.getStatusCode() == 200l) {
				return true;
				//return parser.parseTicketSearchResponse(response);
			} else if (response.getStatusCode() == 401l) {
				//throw new CredentialException(response.getStatusMessage());
			} else {
				throw new IOException(String.format("Server returned REST-response code %s (%s)", response.getStatusCode(), response.getStatusMessage()));
			}
		} else {
			throw new UnsupportedOperationException("Could not create parser for response format.");
		}

		return false;
	}
	

	// getter and setter methods...
	public RTRESTClient getClient() {
		return client;
	}
	public void setClient(RTRESTClient client) {
		this.client = client;
	}

	/** This actually probably isn't necessary for our purposes but here it is **/
	@Override
	public boolean editTicket(RTTicket ticket, Map<String, String> parameters)
			throws Exception {
		client.login();
		RTRESTResponse response = client.editTicket(ticket, parameters);
		client.logout();

		if (response.getStatusCode() == 200l) {
			return true;
		} else {
			// failed!!
			return false;
		}
	}

	@Override
	public boolean commentOnTicket(RTTicket ticket, Map<String, String> parameters)
			throws Exception {
		client.login();
		RTRESTResponse response = client.commentOnTicket(ticket, parameters);
		client.logout();

		if (response.getStatusCode() == 200l) {
			return true;
		} else {
			// failed!!
			return false;
		}
	}

		private static List<RTTicket> processResultData(List<Map<String,String>> resultData) {
		List<RTTicket> result = new LinkedList<RTTicket>();

		for (Map<String,String> ticketData : resultData) {
			RTTicket rtTicket= processTicketData(ticketData);

				if(rtTicket.getId() != null)
					result.add(rtTicket);

		}

		return result;
	}

	private static RTTicket processTicketData(Map<String,String> ticketData) {
		RTTicket ticket = new RTTicket();

		FieldProcessor fieldProcessor = null;

		for (Map.Entry<String,String> e : ticketData.entrySet()) {
			if(e.getKey() != null) {
				fieldProcessor = FieldProcessorRegistry.getInstance().getTicketFieldProcessor(ticket, e.getKey());
				fieldProcessor.process(ticket, e.getKey(), e.getValue());
			}
		}

		return ticket;
	}

	@Override
	public String findByQueryAsJsonString(String query) throws IOException {
		return findByQueryAsJsonString(query, null, null);
	}
	
	@Override
	public String findByQueryAsJsonString(String query, String orderby) throws IOException {
		return findByQueryAsJsonString(query, orderby, null);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public String findByQueryAsJsonString(String query, String orderby, String fields) throws IOException {
		client.login();
		RTRESTResponse response = client.searchTickets(query, orderby, fields, TicketSearchResponseFormat.MULTILINE);
		client.logout();
		
		String body = response.getBody();
		
		Scanner sc = new Scanner(body);
    	
    	JSONArray objects = new JSONArray();
    	JSONObject current = new JSONObject();
    	
    	while (sc.hasNext()) {
    		String line = sc.nextLine();
    		
    		if (line.trim().isEmpty())
    			continue;
    		
    		if (line.trim().equals("--")) {
    			objects.add(current);
    			current = new JSONObject();
    		}
    		
    		int pos = line.indexOf(':');
    		if (pos < 1)
    			continue;
    		String key = line.substring(0,pos);
    		String value = line.substring(pos+1);
    		current.put(key, value);
    		
    	}
    	
    	if (current.size()>0)
    		objects.add(current);
    	
    	sc.close();
    	
    	return objects.toJSONString();
		
	}
}
