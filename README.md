PAM: Probabilistic API Miner [![Build Status](https://travis-ci.org/mast-group/api-mining.svg?branch=master)](https://travis-ci.org/mast-group/api-mining)
================
 
PAM is a near parameter-free probabilistic algorithm for mining the most interesting API patterns from a list of API call sequences. PAM largely avoids returning redundant and spurious sequences, unlike API mining approaches based on frequent pattern mining.

This is an implementation of the API miner from our paper:  
[*Parameter-Free Probabilistic API Mining across GitHub*](http://arxiv.org/abs/1512.05558)  
J. Fowkes and C. Sutton. FSE 2016.   


Installation 
------------

#### Installing in Eclipse

Simply import as a maven project into [Eclipse](https://eclipse.org/) using the *File -> Import...* menu option (note that this requires [m2eclipse](http://eclipse.org/m2e/)). 

It's also possible to export a runnable jar from Eclipse using the *File -> Export...* menu option.

#### Compiling a Runnable Jar

To compile a standalone runnable jar, simply run

```
mvn package
```

in the top-level directory (note that this requires [maven](https://maven.apache.org/)). This will create the standalone runnable jar ```api-mining-1.0.jar``` in the api-mining/target subdirectory. The main class is *apimining.pam.main.PAM* (see below).


Running PAM
-----------

PAM uses a probabilistic model to determine which API patterns are the most interesting in a given dataset.  

#### Mining API Patterns 

Main class *apimining.pam.main.PAM* mines API patterns from a specified API call sequence file. It has the following command line options:

* **-f**  &nbsp;  API call sequence file to mine (in [ARFF](https://weka.wikispaces.com/ARFF+%28stable+version%29) format, see below)
* **-o**  &nbsp;  output file
* **-i**  &nbsp;  max. no. iterations
* **-s**  &nbsp;  max. no. structure steps
* **-r**  &nbsp;  max. runtime (min)
* **-l**  &nbsp;  log level (INFO/FINE/FINER/FINEST)
* **-v**  &nbsp;  log to console instead of log file   

See the individual file javadocs in *apimining.pam.main.PAM* for information on the Java interface.
In Eclipse you can set command line arguments for the PAM interface using the *Run Configurations...* menu option. 

#### Example Usage

A complete example using the command line interface on a runnable jar. We can mine the provided dataset ```netty.arff``` as follows: 

  ```sh 
  $ java -jar api-mining/target/api-mining-1.0.jar -i 1000 -f datasets/calls/all/netty.arff -o patterns.txt -v 
  ```

which will write the mined API patterns to ```patterns.txt```. Omitting the ```-v``` flag will redirect logging to a log file in ```/tmp/```. 

Input/Output Formats
--------------------

#### Input Format

PAM takes as input a list of API call sequences in [ARFF](https://weka.wikispaces.com/ARFF+%28stable+version%29) file format
The ARFF format is very simple and best illustrated by example. The first few lines from ```netty.arff``` are:

```text
@relation netty

@attribute fqCaller string
@attribute fqCalls string

@data
'com.torrent4j.net.peerwire.AbstractPeerWireMessage.write','io.netty.buffer.ChannelBuffer.writeByte'
'com.torrent4j.net.peerwire.messages.BitFieldMessage.writeImpl','io.netty.buffer.ChannelBuffer.writeByte'
'com.torrent4j.net.peerwire.messages.BitFieldMessage.readImpl','io.netty.buffer.ChannelBuffer.readable io.netty.buffer.ChannelBuffer.readByte'
'com.torrent4j.net.peerwire.messages.BlockMessage.writeImpl','io.netty.buffer.ChannelBuffer.writeInt io.netty.buffer.ChannelBuffer.writeInt io.netty.buffer.ChannelBuffer.writeBytes'
'com.torrent4j.net.peerwire.messages.BlockMessage.readImpl','io.netty.buffer.ChannelBuffer.readInt io.netty.buffer.ChannelBuffer.readInt io.netty.buffer.ChannelBuffer.readableBytes io.netty.buffer.ChannelBuffer.readBytes'
```

The ```@relation``` declaration names the dataset and the following two ```@attribute``` statements declare that the dataset consists of two comma separated attributes:   
* ```fqCaller``` &nbsp; the fully-qualified name of the client method, enclosed in single quotes  
* ```fqCalls``` &nbsp; a space-separated list of fully-qualified names of API method calls, enclosed in single quotes.

The dataset is listed after the ```@data``` relation: each line contains a specific method (```fqCaller```) and its API call 
sequence (```fqCalls```). Note that the ```fqCaller``` attribute can be empty for PAM and UPMiner, it is only required for MAPO (see below).
Note that while this example uses Java, PAM is language agnostic and can use API call sequences from *any* language.

#### Output Format

PAM outputs a list of the most interesting API call patterns (i.e. subsequences of the original API call sequences) ordered by their probability under the model. 
For example, the first few lines in the output file ```patterns.txt``` for the usage example above are:

```text
prob: 0.04878
[io.netty.channel.Channel.write]

prob: 0.04065
[io.netty.channel.ExceptionEvent.getCause, io.netty.channel.ExceptionEvent.getChannel]

prob: 0.04065
[io.netty.channel.ChannelHandlerContext.getChannel]

prob: 0.03252
[io.netty.channel.Channel.close]
```

See the accompanying [paper](http://arxiv.org/abs/1512.05558) for details.


Java API Call Extractor
-----------------------

The class *apimining.java.APICallExtractor* contains our 'best-effort' API call sequence extractor for Java source files. 
We used it to create the API call sequence datasets for our paper.  

It takes folders of API client source files as input and generates API call sequences files (in ARFF format) for each API library given. For best performance, it requires a folder of namespaces used in the libraries so that it can resolve wildcarded namespaces. These can be collected using the provided Wildcard Namespace Collector class: *apimining.java.WildcardNamespaceCollector*. 

See the individual class javadocs in *apimining.java* for details of their use.


MAPO and UPMiner Implementations
--------------------------------

For comparison purposes, we implemented the API miners [MAPO](https://www.cs.sfu.ca/~jpei/publications/Mapo-ecoop09.pdf) and [UPMiner](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/07/miningsuccincthighcoverageapiusagepatternsfromsourcecode.pdf) from stratch using the [Weka](http://www.cs.waikato.ac.nz/ml/weka/) hierarchical clusterer. These are provided in the 
*apimining.mapo.MAPO* and *apimining.upminer.UPMiner* classes respectively. They have the following command line options:

* **-f**  &nbsp;  API call sequence file to mine (in [ARFF](https://weka.wikispaces.com/ARFF+%28stable+version%29) format, see above)
* **-o**  &nbsp;  output folder
* **-s**  &nbsp;  minimum support threshold

See the individual class files for information on the Java interface. Note that these are not particularly fast implementations as Weka's hierarchical clusterer is rather slow and inefficient. Moreover, as both API miners are based on frequent pattern mining algorithms, they can suffer from pattern explosion (this is a known problem with frequent pattern mining algorithms).


Datasets
--------

AAll datasets used in the paper are available in the ```datasets/``` subdirectory: 
* ```datasets/calls/all``` contains API call sequences for each of the 17 Java libraries described in our [paper](http://arxiv.org/abs/1512.05558) (see Table 1)
* ```datasets/calls/train``` contains the subset of API call sequences used as the 'training set' in the paper
 
Both datasets use the [ARFF](https://weka.wikispaces.com/ARFF+%28stable+version%29) file format described above. In addition, so that it is possible to replicate our evaluation, we have provided the Java source files for:
* each of the library client classes in ```datasets/source/client_files.tar.xz``` 
* the library example classes in ```datasets/source/example_files.tar.xz``` 
* the namespaces necessary for our *API Call Extractor* in ```namespaces.tar.xz```

Finally, the ```datasets/source/test_train_split``` subdirectory details the training/test set assignments for each client class.


Bugs
----

Please report any bugs using GitHub's issue tracker.


License
-------

This algorithm is released under the GNU GPLv3 license. Other licenses are available on request.
