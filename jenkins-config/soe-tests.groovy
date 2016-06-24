
for(x in 0..6) {

  job("Test ${x}")
    steps {
      shell("echo Test ${x}")
    } 
  }
}
