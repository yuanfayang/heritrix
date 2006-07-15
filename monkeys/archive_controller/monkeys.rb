#!/usr/bin/env ruby

# This is a web interface for testing the Monkey Controller.
# To use it you have to install the Camping web framework:
#
# 1. Install ruby
# 2. Install ruby-gems
# 3. Run 'gem install camping'
# 
# Now to start the web interface simply run 'camping monkeys.rb'
#
# Note: don't forget to update the monkey controller port below if you
# changed it from the default

$:.unshift File.dirname(__FILE__) + "/../../lib"
require 'rubygems'
require_gem 'camping', '>=1.4'
require 'camping/session'
require 'net/http'
require 'URI'
require 'json'

Camping.goes :Monkeys

module Monkeys::Helpers
  def self.create_query(method, data)
    query = "\r\n" + data
    headers = {'Content-Type' => 'text/plain', 
      'Accept' => '*/*'}
    return query, headers
  end
end

# config
module Monkeys
  CONTROLLER_PORT = 8081
end

module Monkeys
  include Camping::Session
end

module Monkeys::Controllers
  class Index < R '/'
    def get
      render :index
    end
  end

  class AddTask
    def get
      render :add_task
    end

    def post
      begin
        url = URI.parse("http://localhost:#{Monkeys::CONTROLLER_PORT}")
        taskDataJson = JSON.unparse({'URL' => @input.task_url})
        Net::HTTP.start(url.host, url.port) do |http|
          q, h = Monkeys::Helpers::create_query('submitTask', taskDataJson)
          res = http.post('/admin?method=submitTask', q, h)
          resBody = res.body.strip()
          @state.msg = "Status Code: [#{res.code}], Message: [#{res.message}], Body: [#{resBody}]"
        end
      rescue Exception => e
        @state.msg = "An error has occured: [#{e}]"
      end
      redirect Index
    end
  end
  
  class TaskStatus
    def get
      render :task_status
    end
    
    def post
      id = @input.tid
      url = URI.parse("http://localhost:#{Monkeys::CONTROLLER_PORT}")
      Net::HTTP.start(url.host, url.port) do |http|
        res = http.get("/admin?method=getTaskStatus&tid=#{id}")
        resBody = res.body.strip()
        @state.msg = "Status Code: [#{res.code}], Message: [#{res.message}], Body: [#{resBody}]"
      end
      redirect Index
    end
  end
  
  class FreeTasks
    def get
      url = URI.parse("http://localhost:#{Monkeys::CONTROLLER_PORT}")
      Net::HTTP.start(url.host, url.port) do |http|
        res = http.get("/admin?method=showQueue")
        #puts res.body
        @task_url_list = JSON.parse(res.body).collect do |x|
          x['URL']
        end
      end
      render :task_list
    end
  end
  
  class Style < R '/styles.css'
    def get
      @headers["Content-Type"] = "text/css; charset=utf-8"
      @body = %{
                body {
                    font-family: Utopia, Georga, serif;
                }
                h1.header {
                    background-color: #fef;
                    margin: 0; padding: 10px;
                }
                div.content {
                    padding: 10px;
                }
            }
    end
  end
end

module Monkeys::Views
  def layout
    html do
      head do
        title 'Monkey controller'
        link :rel => 'stylesheet', :type => 'text/css', 
        :href => '/styles.css', :media => 'screen'
      end
      body do
        h1.header { a 'Monkey controller', :href => R(Index) }
        div.content do
          self << yield
        end
      end
    end
  end

  def index
    if @state.msg
      p { text @state.msg }
      @state.msg = nil
    end
    h2 { 'Commands' }
    p {
      ul {
        li { a 'Create new task', :href => R(AddTask) }
        #li { a 'Cancel task', :href => R(CancelTask) }
        li { a 'Get task status', :href => R(TaskStatus) }
        li { a 'Get free tasks', :href => R(FreeTasks) }
        #li { a 'Get assigned tasks', :href => R(AssignedTasks) }
        #li { a 'Get completed tasks', :href => R(CompletedTasks) }
        #li { a 'Get failed tasks', :href => R(FailedTasks) }
      }
    }
  end

  def add_task
    _form(:action => R(AddTask))
  end
  
  def task_status
    h2 { 'Task Status' }
    
    form({:method => 'post', :action => R(TaskStatus)}) do 
      label 'Task ID', :for => 'tid'
      input :name => 'tid', :type => 'text',
      :value => '', :size => '60'
      p { input :type => 'submit' }
    end
  end
  
  def task_list
    h2 { 'Free tasks list' }
    ul do 
      @task_url_list.each do |url|
        li { url }
      end
    end
  end

  # partials
  
  def _form(opts)
    h2 { 'Task data' }
    
    form({:method => 'post'}.merge(opts)) do
      label 'URL', :for => 'task_url'
      input :name => 'task_url', :type => 'text', 
      :value => '', :size => '60'
      p { input :type => 'submit' }
    end
  end
end

if __FILE__ == $0
  require 'mongrel/camping'

  #Monkeys::Models::Base.establish_connection :adapter => 'sqlite3', :database => 'monkeys.db'
  #Monkeys::Models::Base.logger = Logger.new('camping.log')
  #Monkeys::Models::Base.threaded_connections=false
  #Monkeys.create

  server = Mongrel::Camping::start("0.0.0.0",3002,"/monkeys",Monkeys)
  server.run.join
end
