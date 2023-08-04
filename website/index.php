<?php

$fileContent = file_get_contents('command.txt');

if ($fileContent !== "") {
    $command = $fileContent;
    echo $command;
}