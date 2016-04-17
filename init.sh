#!/bin/bash
git submodule deinit -f .
git submodule init
git submodule update
git submodule foreach git checkout master
git submodule foreach git pull
