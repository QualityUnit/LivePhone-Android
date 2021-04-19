<?php
/*
 * Used APIs:
 * http://crowdin.net/page/api/update-file
 * http://crowdin.net/page/api/upload-translation
 * http://crowdin.net/page/api/export
 * http://crowdin.net/page/api/status
 * http://crowdin.net/page/api/download 
 *
 * */

@error_reporting(E_ALL);
@ini_set('display_errors', true);
@ini_set('display_startup_errors', true);
@ini_set('default_socket_timeout', 5);

define('RES_PATH', '../app/src/main/res/');
define('LANG_LIMIT', 50); // new language need to be translated to 50% 
define('PROJECT_ID', 'liveagentqualityunit'); // project identifier

if (empty($argv[1])) {
	die("Param apikey is missing.");
} else {
    define('API_KEY', $argv[1]); // crowdin project API key
}

uploadSource();
uploadTranslations();
buildLanguages();

try {
	languageStatusAndDownload ();
	echo "\nDownload accomplished!\n\n";
} catch (Exception $e) {
	die("There were some errors. ". $e->getMessage());
}

function uploadSource() {
	$request_url = 'https://api.crowdin.com/api/project/' . PROJECT_ID . '/update-file?key=' . API_KEY;

  $strings_file = new CURLFile(realpath(RES_PATH . 'values/strings.xml'),'application/xml','strings.xml');
  
	$post_params = array();
	$post_params['files[mobile/livephone-android/strings.xml]'] = $strings_file;
	
	try {
		echo "Uploading default android strings file to Crowdin...\n";
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_URL, $request_url);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
		curl_setopt($ch, CURLOPT_POST, true);
		curl_setopt($ch, CURLOPT_POSTFIELDS, $post_params);
		
		$result = curl_exec($ch);
		curl_close($ch);
		echo $result;

		$xml = new SimpleXMLElement($result);
		foreach ($xml->stats->file as $file) {
			if ($file['status'] == "updated") {
				echo "Default file ".$file['name']." on Crowdin updated!\n";
			} elseif ($xml->stats->file['status'] == "skipped") {
				echo "Update of ".$file['name']." skipped. No new strings in file, so no update needed.\n";
			} else {
				echo $file['name']." :: some PROBLEM there!!!\n";
			}
		}
	} catch (Exception $e) {
		die('Error: '.$e->getMessage());
	}
}

function uploadTranslations() {
  $request_url = 'https://api.crowdin.com/api/project/' . PROJECT_ID . '/upload-translation?key=' . API_KEY;
  
	$strings_file = new CURLFile(realpath(RES_PATH . 'values/strings.xml'),'application/xml','strings.xml');
  
	$post_params = array();
	$post_params['files[mobile/livephone-android/strings.xml]'] = $strings_file;
  $post_params['language'] = "en";
  $post_params['auto_approve_imported'] = "1";
  $post_params['import_eq_suggestions'] = "1";

  try {
      echo "Uploading translation file to Crowdin...\n";
      $ch = curl_init();
      curl_setopt($ch, CURLOPT_URL, $request_url);
      curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
      curl_setopt($ch, CURLOPT_POST, true);
      curl_setopt($ch, CURLOPT_POSTFIELDS, $post_params);
  
      $result = curl_exec($ch);
      curl_close($ch);
  
      $xml = new SimpleXMLElement($result);
      if (!isset($xml->code)) {
          echo "Default language translations on Crowdin updated!\n\n";
      } else {
          print_r('Some unknown PROBLEM there: '.$result);
      }
  } catch (Exception $e) {
      die('Error: '.$e->getMessage());
  }
}

function buildLanguages() {
	$request_url = 'https://api.crowdin.com/api/project/' . PROJECT_ID . '/export?key=' . API_KEY;
	
	try {
		echo "Need to build? Waiting for result...\n";
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_URL, $request_url);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
		$result = curl_exec($ch);
		curl_close($ch);
		
		$xml = new SimpleXMLElement($result);
		echo "Status: ".$xml['status']."\n"; // can be "built" or "skipped" /no change from last build or there was a build in past 30mins/
	} catch (Exception $e) {
		die('Error: '.$e->getMessage());
	}
}

function packageMapping($package) {
	$packageMappings = array(
			'sr-CS' => 'sr',
			'de-DE' => 'de',
			'it-IT' => 'it',
			'mk-MK' => 'mk',
			'nl-NL' => 'nl',
			'vls-BE' => 'nl-rBE',
			'pl-PL' => 'pl',
			'pt-BR' => 'pt',
			'es-ES' => 'es',
			'sv-SE' => 'sv',
			'zh-CN' => 'zh',
			'zh-TW' => 'zh-rTW',
	);
	if (array_key_exists($package.'', $packageMappings)) {
		return $packageMappings[$package.''];
	}
	return $package;
}

function deleteDir($dirPath) {
	if (! is_dir($dirPath)) {
		throw new InvalidArgumentException("$dirPath must be a directory");
	}
	if (substr($dirPath, strlen($dirPath) - 1, 1) != '/') {
		$dirPath .= '/';
	}
	$files = glob($dirPath . '*', GLOB_MARK);
	foreach ($files as $file) {
		if (is_dir($file)) {
			deleteDir($file);
		} else {
			unlink($file);
		}
	}
	rmdir($dirPath);
}

function myUnlink($file) {
	if(file_exists($file)) {
		unlink($file);
	} else {
// echo "No need to unlink file.\n";
	}
}

function updateTranslations($sourceFile,$package) {
	$scriptPath = realpath(dirname(__FILE__));
	$stringsFile = filesize($scriptPath . 'temp/mobile/livephone-android/'.$sourceFile);
	if ($stringsFile != 84) {
		echo "Updating ".$sourceFile."..\n";
		myUnlink(RES_PATH . 'values-'.$package.'/'.$sourceFile);
		if (!file_exists(RES_PATH . 'values-'.$package)) {
			mkdir(RES_PATH . 'values-'.$package, 0777, true);
		}
		rename($scriptPath . 'temp/mobile/livephone-android/'.$sourceFile, RES_PATH . 'values-'.$package.'/'.$sourceFile);
		myUnlink($scriptPath . 'temp/mobile/livephone-android/'.$sourceFile);
	} else {
		echo "File ".$sourceFile." hasn't any translated strings. Skipping...\n";
	}
}

function downloadLanguage($package) {
	try {
		echo "Downloading 'la_".$package.".zip ...'\n";
		$languageFile = file_get_contents("https://api.crowdin.com/api/project/" . PROJECT_ID . "/download/".$package.".zip?key=". API_KEY);
		$package = packageMapping($package);
		
		$saveFile = file_put_contents("la_".$package.".zip", $languageFile);
		echo "Downloaded!\n";
		//chmod ("la_".$package.".zip", 0777);
		
		$scriptPath = realpath(dirname(__FILE__));
		$zipFile = new ZipArchive();
		$zipFile->open("la_".$package.".zip");
		$zipFile->extractTo($scriptPath . 'temp/');
		$zipFile->close();
		myUnlink('la_'.$package.'.zip');
		
		updateTranslations('strings.xml',$package);
		
		deleteDir($scriptPath . 'temp/');
	
	} catch (Exception $e) {
		exit ("Download failed! Error: ".$e->getMessage());
	}
}

function languageStatusAndDownload() {
	//Load status of each language --> name, code, % of translation
	$request_url = 'https://api.crowdin.com/api/project/' . PROJECT_ID . '/status?key=' . API_KEY . '&xml';
	try {
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_URL, $request_url);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
		$result = curl_exec($ch);
		curl_close($ch);
	} catch (Exception $e) {
		die('Error: '.$e->getMessage());
	}

	
	$xml = new SimpleXMLElement($result);
	foreach ($xml->language as $lang) {
		if ($lang->name != "English") {
			try {
				downloadLanguage($lang->code);
			} catch (Exception $e) {
				exit ($e->getMessage);
			}
		}
	}
}