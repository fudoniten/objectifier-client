{
  description = "Objectifier Client";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    helpers = {
      url = "git+https://fudo.dev/public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, helpers, utils, ... }:
    utils.lib.eachDefaultSystem (system: {
      packages = rec {
        default = objectifier-client;
        objectifier-client = helpers.packages."${system}".mkClojureBin {
          name = "org.fudo/objectifier-client";
          primaryNamespace = "objectifier-client.cli";
          src = ./.;
        };
      };

      devShells = let pkgs = import nixpkgs { inherit system; };
      in rec {
        default = objectifier-client;
        objectifier-client = pkgs.mkShell {
          buildInputs = [ self.packages."${system}".objectifier-client ];
        };
        update-deps = pkgs.mkShell {
          buildInputs = with helpers.packages."${system}";
            [ updateClojureDeps ];
        };
      };
    });
}
