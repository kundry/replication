require 'net/http'
require 'uri'
require 'json'

class Test 
  def initialize
    uri = URI.parse("http://localhost:8080/events/2/purchase/2765")
    header = {'Content-Type': 'text/json'}
    event = {tickets:50}
   
    # Create the HTTP objects
    http = Net::HTTP.new(uri.host, uri.port)
    request = Net::HTTP::Post.new(uri.request_uri, header)
	  request.body = event.to_json

	  # Send the request
	  response = http.request(request)
	
  end
end

# initialize object
Test.new