:page/title nix
:blog-post/tags [:nix]
:blog-post/author {:person/id :jan}
:page/body

## nix-shell
`nix-shell -p program-you-like-to-use` 就会进入一个 shell 是安装好 program-you-like-to-use 的.  
也可以单独只跑一次, 然后退出
`nix-shell -p program --run args`
也可以提供多个 program 一起用
`nix-shell -p git nodejs`

program 都有哪些可以在 https://search.nixos.org/packages 上面搜索

执行的时候根据当前的 nix 版本可能会获取到不同的 program / package. 可以通过 -I 参数指定一个固定的版本.

目前用 nix-shell 来 setup 新的机器, 就不需要逐个安装 node java clojure zsh git docker 之类的了. 