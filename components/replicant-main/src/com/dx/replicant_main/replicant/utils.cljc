(ns com.zihao.replicant-main.replicant.utils
  (:require
   [clojure.walk :as walk]
   #?(:clj [clojure.java.io :as io]))
  #?(:clj (:import
           [javax.crypto Cipher KeyGenerator SecretKey]
           [javax.crypto.spec SecretKeySpec]
           [java.security SecureRandom])))

#?(:clj
   (defn generate-aes-key []
     (let [key-gen (KeyGenerator/getInstance "AES")]
       (.init key-gen 128 (SecureRandom.)) ; 128-bit key
       (.generateKey key-gen))))

#?(:clj
   (defn encrypt 
     "return base64 string"
     [^SecretKey key ^bytes plaintext]
     (let [cipher (Cipher/getInstance "AES")]
       (.init cipher Cipher/ENCRYPT_MODE key)
       (let [bytes (.doFinal cipher plaintext)] 
         (.encodeToString (java.util.Base64/getEncoder) bytes)))))

#?(:clj
   (defn decrypt [^SecretKey key ^String ciphertext]
     (let [ciphertext (.decode (java.util.Base64/getDecoder) ciphertext)
           cipher (Cipher/getInstance "AES")]
       (.init cipher Cipher/DECRYPT_MODE key)
       (let [bytes (.doFinal cipher ciphertext)]
         (String. bytes)))))

(comment
  (def aes-key (generate-aes-key)) 
  (def key-str (.encodeToString (java.util.Base64/getEncoder) (.getEncoded aes-key)))
  (def key-bytes (.decode (java.util.Base64/getDecoder) key-str))

  (SecretKeySpec. key-bytes "AES")

  (def encrypted (encrypt aes-key (.getBytes "hello"))) 
  (def decrypted (decrypt (SecretKeySpec. key-bytes "AES") encrypted)) 

  :rcf)


(defn parse-int [s]
  #?(:clj (Integer/parseInt (str s))
     :cljs (js/parseInt s 10)))

(comment
  (js/parseInt "10" 10)
  :rcf)

(defn is-digit? [c]
  #?(:clj (Character/isDigit c)
     :cljs (let [n (parse-int c)]
             (and
              (not (js/isNaN n))
              (number? n)))))

(defn gather-form-data [form-el]
  #?(:clj form-el
     :cljs (some-> (js/FormData. form-el)
                   into-array
                   (.reduce
                    (fn [res [key value]]
                      (assoc res (keyword key) value))
                    {}))))

(defn make-interpolate
  "Creates an interpolation function for handling event data with optional extensions.
   
   Parameters:
   - extension-fns: Optional functions to extend the interpolation behavior.
                    Each function should take (event case-key) and return a value if handled, nil otherwise.
   
   Returns: An interpolation function that can be used to process event data"
  [& extension-fns]
  (fn [event data]
    (walk/postwalk
     (fn [x]
       (let [result (or
                     (some #(when-let [result (% event x)]
                              result)
                           extension-fns)
                     (case x
                       :event/target.value (.. event -target -value)
                       :event/target.int-value (parse-int (.. event -target -value))
                       :event/target.checked (.. event -target -checked)
                       :event/clipboard-data (.getData (.. event -clipboardData) "text")
                       :event/target (.. event -target)
                       :event/form-data (some-> event .-target gather-form-data)
                       :event/event event
                       :event/file (or (when-let [files (.-files (.-target event))]
                                         (aget files 0))
                                       :event/file)
                       :query/result event
                       nil))]
         (if (some? result)
           result
           x)))
     data)))

(def interpolate
  "Default interpolation function with no extensions"
  (make-interpolate))

#?(:clj
   (defn read-and-remove-first-line
     "Thread-safe function to read the first line from a file and remove it.
   Returns a map with :success boolean and :line string or :error string.
   Uses file locking to ensure thread safety."
     [file-path]
     (let [file (io/file file-path)]
       (if-not (.exists file)
         nil
         (locking file
           (let [lines (with-open [reader (io/reader file)]
                         (doall (line-seq reader)))
                 first-line (first lines)
                 remaining-lines (rest lines)]
             (if (nil? first-line)
               nil
               (do
                 (with-open [writer (io/writer file)]
                   (doseq [line remaining-lines]
                     (.write writer (str line "\n"))))
                 first-line))))))))

#?(:clj
   (defmacro build-admin? []
     true
     #_(= "ADMIN" (System/getenv "BUILD"))))
