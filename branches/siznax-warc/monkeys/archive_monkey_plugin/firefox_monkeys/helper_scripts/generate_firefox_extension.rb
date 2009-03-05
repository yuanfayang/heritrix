=begin
== Firefox extension stub generator
= Usage
Just run:

<pre>
ruby generate_firefox_extension.rb <root dir where extension dir will go> <extension name>
</pre>

This will create an extension dir inside the root dir.
The contents are taken from
http://kb.mozillazine.org/Getting_started_with_extension_development
=end

module FirefoxExtensionStubGenerator
  FOLDERS = %w(/content /locale /locale/en-US /skin)

  CHROME_REGISTRATION = <<-END
    content helloworld content/
    overlay chrome://browser/content/browser.xul chrome://helloworld/content/overlay.xul
    
    locale helloworld en-US locale/en-US/
    
    skin helloworld classic/1.0 skin/
    style chrome://global/content/customizeToolbar.xul chrome://helloworld/skin/overlay.css
  END
  
  OVERLAY_XUL = <<-END
<?xml version="1.0"?>
<?xml-stylesheet href="chrome://helloworld/skin/overlay.css" type="text/css"?>
    <!DOCTYPE overlay SYSTEM "chrome://helloworld/locale/overlay.dtd">
    <overlay id="helloworld-overlay"
        xmlns="http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul">
      <script src="overlay.js"/>
    
      <menupopup id="menu_ToolsPopup">
      <menuitem id="helloworld-hello" label="&helloworld;"
        oncommand="HelloWorld.onMenuItemCommand(event);"/>
      </menupopup>
    </overlay> 
  END
  
  OVERLAY_JS = <<-END
    var HelloWorld = {
      onLoad: function() {
        // initialization code
        this.initialized = true;
      },
    
      onMenuItemCommand: function() {
        window.open("chrome://helloworld/content/hello.xul", "", "chrome");
      }
    };
    
    window.addEventListener("load", function(e) { HelloWorld.onLoad(e); }, false); 
  END
  
  OVERLAY_DTD = '<!ENTITY helloworld "Hello World!">'
  
  HELLO_DTD = <<-END
    <!ENTITY title.label "Hello World">
    <!ENTITY separate.label "This is a separate window!">
    <!ENTITY close.label "Close">  
  END
  
  HELLO_XUL = <<-END
<?xml version="1.0"?>
<?xml-stylesheet href="chrome://global/skin/global.css"  type="text/css"?>
    <!DOCTYPE window SYSTEM "chrome://helloworld/locale/hello.dtd">
    
    <window xmlns="http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul" 
            title="&title.label;">
    
    <hbox align="center">
      <description flex="1">&separate.label;</description>
      <button label="&close.label;" oncommand="close();"/>
    </hbox>
     
     </window>  
  END

  INSTALL_RDF = <<-END
<?xml version="1.0"?>
    <RDF xmlns="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
         xmlns:em="http://www.mozilla.org/2004/em-rdf#">
    
      <Description about="urn:mozilla:install-manifest">
      
        <em:id>helloworld@mozilla.doslash.org</em:id>
        <em:name>Hello World (Firefox 1.5 edition)</em:name>
        <em:version>1.0</em:version>
        <em:description>Classic first extension from MozillaZine KB</em:description>
        <em:creator>Nickolay Ponomarev</em:creator>
        <!-- optional items -->
        <em:contributor>A person who helped you</em:contributor>
        <em:contributor>Another one</em:contributor>
        
        <em:homepageURL>http://kb.mozillazine.org/Getting_started_with_extension_development</em:homepageURL>
        <!-- em:optionsURL>chrome://sampleext/content/settings.xul</em:optionsURL>
        <em:aboutURL>chrome://sampleext/content/about.xul</em:aboutURL>
        <em:iconURL>chrome://sampleext/skin/mainicon.png</em:iconURL>
         
        <em:updateURL>http://sampleextension.mozdev.org/update.rdf</em:updateURL-->
     
        <!-- Firefox -->
        <em:targetApplication>
          <Description>
            <em:id>{ec8030f7-c20a-464f-9b0e-13a3a9e97384}</em:id>
            <em:minVersion>1.5</em:minVersion>
            <em:maxVersion>1.5.0.*</em:maxVersion>
          </Description>
        </em:targetApplication>
     
      </Description>
     
    </RDF> 
  END

  STUB_FILES = {
    '/chrome.manifest' => CHROME_REGISTRATION, 
    '/install.rdf' => INSTALL_RDF,
    '/content/overlay.js' => OVERLAY_JS,
    '/content/overlay.xul' => OVERLAY_XUL,
    '/content/hello.xul' => HELLO_XUL,
    '/locale/en-US/overlay.dtd' => OVERLAY_DTD,
    '/locale/en-US/hello.dtd' => HELLO_DTD,
    '/skin/overlay.css' => ''
  }

  def FirefoxExtensionStubGenerator::generate_stub(root_dir, extension_name)
    # generate the dir structure
    extension_root = root_dir + extension_name
    Dir::mkdir(extension_root)
    FOLDERS.each {|f| puts "Creating dir #{extension_root + f}"; Dir::mkdir(extension_root + f)}
    
    # generate stub files
    STUB_FILES.each do |k,v|
      puts "Writing stub #{extension_root + k}"
      File.open(extension_root + k, 'w') {|f| f.write(v)}
    end
    
    puts "done"
  end
  
end

if __FILE__ == $0
  if ARGV.size != 2
    puts "ruby generate_firefox_extension.rb <root dir where extension dir will go> <extension name>"
  else
    FirefoxExtensionStubGenerator.generate_stub(ARGV[0], ARGV[1])
  end
end