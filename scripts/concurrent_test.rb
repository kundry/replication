require 'net/http'
require 'uri'
require 'json'

class Test 
  def initialize
   
	 threads = [] 
	 10.times {
	 	threads << Thread.new {
	 		uri = URI.parse("http://localhost:8085/events/4/purchase/2766")
    		header = {'Content-Type': 'text/json'}
    		event = {tickets:10}
   
    		# Create the HTTP objects
    		http = Net::HTTP.new(uri.host, uri.port)
    		request = Net::HTTP::Post.new(uri.request_uri, header)
	  		request.body = event.to_json

	  		# Send the request
	  		response = http.request(request)
	 	}
	 }

	 threads.each(&:join)
 
  end
end

# initialize object
Test.new