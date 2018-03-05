package main

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"path"
	"path/filepath"
)

func main() {
	logDir := "./logs"
	imgDir, _ := filepath.Abs("./img/")
	fmt.Println(imgDir)
	http.HandleFunc("/logs", func(w http.ResponseWriter, r *http.Request) {
		f := readAndBufferFiles(logDir)
		w.Write(f.Bytes())
	})
	http.Handle("/", http.FileServer(http.Dir(imgDir)))

	http.HandleFunc("/train", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPost && r.Header["Content-Type"][0] == "image/jpeg" {
			if body, err := r.GetBody; err == nil {
				ioutil.WriteFile()
			} else {
				w.WriteHeader(http.StatusBadRequest)
			}
		} else {
			w.WriteHeader(http.StatusMethodNotAllowed)
		}
	})
	http.ListenAndServe(":8080", nil)
}

func readAndBufferFiles(logDir string) *bytes.Buffer {
	buf := new(bytes.Buffer)
	filepath.Walk(logDir, func(p string, info os.FileInfo, err error) error {
		if path.Ext(p) == ".log" {
			b, err := ioutil.ReadFile(p)
			if err == nil {
				buf.Write(b)
			} else {
				fmt.Println(err.Error())
				return err
			}
		}
		return nil
	})
	return buf
}

func train()
