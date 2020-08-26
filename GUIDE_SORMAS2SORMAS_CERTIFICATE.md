# How to create and add certificates?

This guide explains how to:
 * create a new self-signed certificate, used for SORMAS to SORMAS communication
 * set up the server address list file
 * add certificates of other SORMAS instances to the local truststore
 * add other servers to the local server list
   
### Prerequisites

Java is needed, because the keytool is used for certificate import. <br/>
See [Installing Java](SERVER_SETUP.md#java-11)

### Using the certificate generation script

1. Run ``bash ./generate-cert.sh``
2. If the ``sormas2sormas`` directory is not found, you will be prompted to provide its path.
3. For the generation of the certificate, the following data is needed: a password, a *Common Name* (CN) 
    and an *Organization* (O). These may be set in environment variables (recommended), or provided 
    manually as the script executes.
    * The password environment variable should be named ``SORMAS_S2S_CERT_PASS``. Please note that the password has to be 
    at least 6 characters, or you will be prompted for a new one.
    * the *Common Name* environment variable should be named ``SORMAS_S2S_CERT_CN``.<br/>
    **Important**: for Germany, this value should be the SurvNet Code Site. <br/>
    E.g. *2.03.1.01.*
    * the *Organization* (O) environment variable should be named ``SORMAS_S2S_CERT_ORG``.<br/>
    **Important**: for Germany, this value should be the name of the Health Department (Gesundheitsamt) 
    to which the SORMAS instance will be assigned. <br/>
    E.g. *GA Braunschweig*
4. After providing the requested data, the certificate files will be generated. <br/>
   The generated certificate has a validity of 3 years. 
   The certificate files will be available in the root SORMAS directory, in the folder ``/sormas2sormas``.
5. A CSV file containing the access data for this instance will also be generated in the folder ``/sormas2sormas``.
   It will be named ``server-access-data.csv``.
   The file will contain on the first two columns of the first row the Common Name and the Organization, as provided
   when creating the certificate. <br/>
   **Please fill in on the third column the full URL of the server.** <br/>
   You will also have to set up a user for communicating with other SORMAS instances.
6. The generated ``.p12`` file should not be shared with third parties. <br/>
   The generated ``.crt`` file will be verified and shared with other SORMAS instances, from which this instance
   will be able to request data. Conversely, in order to enable other SORMAS instances to request data from this 
   instance, their certificate files should be obtained and added to the local truststore. The ``server-access-data.csv``
   file will also have to be shared so that the access data of this instance is known to other instances. 
   More details can be found in the next section.
7. If the ``SORMAS_PROPERTIES`` environment variable is available, the relevant properties will be 
    automatically set by the script.
    * Else, the properties which need to be added will be displayed in the console after the script finishes executing.
    * Please note these properties and add them to the ``sormas.properties`` file. This should be located in the 
    ``/domains/sormas`` folder.
    * Example output:
    ```
    sormas.properties file was not found. 
    Please add the following properties to the sormas.properties file:
    sormas2sormas.keyAlias=mycertificate
    sormas2sormas.keyPassword=changeit
    ```

### Adding a new certificate to the Truststore

To enable other SORMAS instances to request data from this instance, their certificate must be added to the 
truststore of this instance. Furthermore, the access data of other instances must be added to the local server
list. To complete this setup, please follow the next steps:
1. Run ``bash ./import-to-truststore.sh``
2. If the ``sormas2sormas`` directory is not found, you will be prompted to provide its path.
3. If ``sormas2sormas.truststore.p12`` is not found in the folder ``/sormas2sormas``, it will be created. 
    The truststore password may be provided in an environment variable ``SORMAS_S2S_TRUSTSTORE_PASS`` (recommended), 
    or manually as the script executes.
    * If the ``SORMAS_PROPERTIES`` environment variable is available, the relevant properties will be 
      automatically set by the script. Please note that the password has to be at least 6 characters, or you will be prompted for a new one.
    * Else, the properties which need to be added will be displayed in the console after the script finishes executing.
    * Please note these properties and add them to the ``sormas.properties`` file. This should be located in the 
        ``/domains/sormas`` folder.
     * Example output:
     ```
     sormas.properties file was not found. 
     Please add the following properties to the sormas.properties file:
     sormas2sormas.truststoreName=name
     sormas2sormas.truststorePass=pass
     ```
4. If the server address list file ``server-list.csv`` is not found in the folder ``/sormas2sormas``, it will also be created.
5. If the environment variable ``SORMAS_S2S_TRUSTSTORE_PASS`` is not available, you will be prompted to 
   provide the password for the truststore.
6. You will be prompted to provide the file name of the certificate to be imported. This certificate should be located
in the ``/sormas2sormas`` folder. Please provide the name including the extension. E.g ``mycert.crt``
7. After providing the requested data, the certificate will be imported to the truststore.
8. You should have also received a CSV file with the server access data. From this file, copy the line corresponding to the
    instance you would like to communicate with and add it to the local server address list file. This file is named
    ``server-list.csv`` and is located in the ``/sormas2sormas`` folder. <br/>
    *Note*: You may check that the Common Name and the Organization of the certificate match the ones corresponding to 
    the server in the CSV file.
9. You may now delete the ``.crt`` file.