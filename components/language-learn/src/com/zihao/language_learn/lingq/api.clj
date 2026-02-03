(ns com.zihao.language-learn.lingq.api
  (:require
   [clojure.string :as str]
   [com.zihao.language-learn.lingq.db :as lingq-db]
   [com.zihao.language-learn.fsrs.db :as fsrs-db]
   [com.zihao.language-learn.dictionary.core :as dictionary]
   [com.zihao.language-learn.fsrs.template :as template]))

(defn split-text-preserve-whitespace [text]
  (->> (re-seq #"([a-zA-Z0-9]+)|(\s+)|([.。,，!！?？;；:：“”'\"\[\]\(\)\{\}<>-])" text)
       (map (fn [[_match word ws punct]]
              (or word ws punct)))
       (remove empty?)))

(comment
  (split-text-preserve-whitespace "Hello, world!")
  :rcf)

(defn tokenize-text [language text]
  (let [tokens (if (= language "zh")
                 ["测试" "中文"]
                 (split-text-preserve-whitespace text))]
    tokens))

(defn query-handler [_system query]
  (case (:query/kind query)
    :query/tokenize-text
    (let [{:keys [language text]} (:query/data query)]
      (tokenize-text language text))

    :query/get-article
    "Judul: Teknologi di Rumahku
    Artikel pendek – level A2-B1
    Pagi ini saya bangun bukan karena alarm handphone, tapi karena lampu kamar otomatis menyala. Lampu itu pintar; ia tahu jam enam adalah waktunya bangun. Teknologi ini namanya “lampu cerdas”. Lampu itu terhubung ke aplikasi kecil di handphone saya. Di aplikasi itu saya atur warna dan kecerahan lampu.
    Di dapur, ibu memakai panci pintar. Panci itu bisa menghitung suhu makanan. Jika nasi sudah matang, panci berbunyi “dit!” dan mati sendiri. Ibu senang karena nasi tidak pernah gosong lagi.
    Ayah pergi ke kantor dengan mobil listrik. Mobil itu tidak pakai bensin, tapi pakai baterai besar. Di garasi ada “stasiun pengisian” yang berbentuk seperti kotak kecil. Cukup enam jam, baterai penuh lagi. Mobil ini tidak mengeluarkan asap, jadi udaranya tetap bersih.
    Saya sekolah daring di kamar. Guru mengirim tugas melalui aplikasi “Kelas Digital”. Kami tidak perlu buku tebal; semua ada di tablet. Jika saya tidak mengerti, saya tekan tombol tanda tanya. Lima menit kemudian, robot kecil menjawab dengan suara ramah, “Halo, ada yang bisa dibantu?”
    Siang hari, robot penyedot debu bekerja. Ia berjalan sendiri di lantai, masuk ke bawah sofa, lalu kembali ke dokingnya untuk mengisi baterai. Seekor kucing kecil saya awalnya takut, tapi sekarang dia tidur di atas robot itu!
    Malam tiba, kami menonton film dengan “kacamata virtual”. Jika saya pakai kacamata ini, rasanya seperti duduk di bioskop. Layar besar muncul di depan mata, padahal kami hanya di ruang tamu.
    Teknologi membuat hidup lebih mudah, tapi kami tetap ingat berbicara langsung, bukan hanya chat. Setelah makan malam, kami menutup semua gawai dan bercanda tanpa layar. Teknologi baik, tapi kebersamaan lebih baik."

    :query/get-word-rating
    (lingq-db/get-word-ratings)

    nil))

(comment
  (query-handler nil {:query/kind :query/get-word-rating})
  :rcf)

(defn command-handler [_system {:command/keys [kind data]}]
  (case kind
    :command/update-word-rating
    (let [{:keys [word rating]} data]
      (fsrs-db/repeat-word! word rating)
      {:message "Card reviewed successfully"})

    :command/add-new-word
    (let [{:keys [word]} data
          en-words (try
                     (dictionary/get-translations word "id" "en")
                     (catch Exception _
                        ;; If translation lookup fails, use the word itself as translation
                       [word]))
          en-text (str/join "," en-words)
          card (template/bhs-indo-card word en-text)]
      (when-not (fsrs-db/by-id-word (:card/id-word card))
        (fsrs-db/save-card! card))
      {:message "Word added successfully"})

    nil))

(comment
  (command-handler nil {:command/kind :command/update-word-rating,
                        :command/data {:word "bahwa", :rating :easy}})
  :rcf)
