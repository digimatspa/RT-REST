package de.boksa.rt.rest.response.parser;

import java.util.List;
import java.util.Map;

import de.boksa.rt.rest.RTRESTResponse;

public interface RTParser {

	public List<Map<String, String>> parseResponse(RTRESTResponse response);

	public List<Map<String, String>> parseResponse(String response);

	public List<Map<String, String>> processMultiLine(String responseBody);

	public List<Map<String, String>> processSingleLine(String responseBody);

	public List<Map<String, String>> parseTicketSearchResponse(String response);
}

