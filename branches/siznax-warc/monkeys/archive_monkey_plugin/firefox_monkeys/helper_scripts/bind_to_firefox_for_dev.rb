=begin
Binds the extension in development to a firefox development profile.
Usage:
<pre>
ruby bind_to_firefox_for_dev.rb <path to dev profile> <path to extension> <extension id>
</pre>
=end

if __FILE__ == $0
  if ARGV.size != 3
    puts "Usage: ruby bind_to_firefox_for_dev.rb <path to dev profile> <path to extension> <extension id>"
  else 
    ext_path = File.expand_path(ARGV[1])
    File.open(ARGV[0] + "/extensions/#{ARGV[2]}", 'w') {|f| f.write("#{ext_path}")}
  end
end